import { admin, assert, findDocuments, googleAccessToken, identifier, member, paths, readDocument, required, serve, sha256, type User } from '../_shared/backend.ts'

async function notifications(body: Record<string, unknown>, user: User) {
  const db = admin()
  assert(user.registered, 403, 'A school account is required.')
  if (body.action === 'register-device' || body.action === 'remove-device') {
    assert(typeof body.token === 'string' && body.token.length >= 20 && body.token.length <= 4096, 400, 'Invalid device token.')
    const id = await sha256(body.token)
    if (body.action === 'remove-device') {
      const result = await db.from('crest_push_devices').delete().eq('id', id).eq('firebase_uid', user.uid)
      if (result.error) throw result.error
    } else {
      assert(body.platform === 'android' || body.platform === 'web', 400, 'Invalid device platform.')
      const result = await db.from('crest_push_devices').upsert({
        id, firebase_uid: user.uid, token: body.token, platform: body.platform, updated_at: new Date().toISOString(),
      })
      if (result.error) throw result.error
    }
    return { ok: true }
  }

  assert(body.action === 'notify-event', 400, 'Unknown notification action.')
  const id = identifier(body.recordId)
  let recipients: string[] = []
  let version = ''
  let title = ''
  let message = ''
  if (body.event === 'group-submitted' || body.event === 'group-reviewed') {
    const group = await readDocument(`${paths.groups}/${id}`)
    assert(group, 404, 'Group not found.')
    version = group.version
    if (body.event === 'group-submitted') {
      assert((user.student && member(group.data, user.uid)) || user.teacher, 403, 'You cannot notify for this group.')
      assert(group.data.uploaded === true && group.data.accepted_research !== true, 409, 'There is no pending submission.')
      recipients = (await findDocuments(paths.teachers, 'access', true)).map(teacher => teacher.id)
      title = 'Research ready for review'
      message = 'A group has submitted research. Open CREST to review it.'
    } else {
      assert(user.teacher, 403, 'An approved teacher account is required.')
      assert(group.data.uploaded === false, 409, 'The review has not been saved yet.')
      assert(group.data.accepted_research === true || (Array.isArray(group.data.comments) && group.data.comments.length > 0), 409, 'There is no saved review update.')
      recipients = Array.isArray(group.data.group_member) ? group.data.group_member.filter((uid): uid is string => typeof uid === 'string') : []
      title = 'Research review updated'
      message = 'Your group has a review update. Open CREST to read it.'
    }
  } else if (body.event === 'account-approved') {
    assert(user.teacher, 403, 'An approved teacher account is required.')
    assert(body.role === 'student' || body.role === 'teacher', 400, 'Invalid account role.')
    const account = await readDocument(`${body.role === 'student' ? paths.students : paths.teachers}/${id}`)
    assert(account && account.data[body.role === 'student' ? 'accepted' : 'access'] === true, 409, 'This account is not approved.')
    version = account.version
    recipients = [id]
    title = 'CREST account approved'
    message = 'Your school account is ready. Open CREST to continue.'
  } else assert(false, 400, 'Invalid notification event.')

  // Claims are derived from saved server state, never from caller-provided text
  // or recipient tokens. Repeated requests for the same version do not spam users.
  const eventId = await sha256(`${body.event}|${body.role || ''}|${id}|${version}`)
  const claim = await db.from('crest_notification_events').insert({ id: eventId })
  if (claim.error?.code === '23505') return { duplicate: true }
  if (claim.error) throw claim.error
  let sent = 0
  let failed = 0
  try {
    const uids = [...new Set(recipients)]
    for (let offset = 0; offset < uids.length; offset += 100) {
      const devices = await db.from('crest_push_devices').select('id, token, platform, firebase_uid').in('firebase_uid', uids.slice(offset, offset + 100))
      if (devices.error) throw devices.error
      for (const device of devices.data) {
        // Data-only messages allow each client to handle permission and foreground
        // display consistently. No private research text is put on the lock screen.
        const response = await fetch(`https://fcm.googleapis.com/v1/projects/${required('CREST_FIREBASE_PROJECT_ID')}/messages:send`, {
          method: 'POST', headers: { Authorization: `Bearer ${await googleAccessToken()}`, 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: {
            token: device.token,
            data: { title, body: message, event: String(body.event), eventId, recipientUid: device.firebase_uid },
            android: { priority: 'HIGH', ttl: '3600s' },
            webpush: { headers: { TTL: '3600', Urgency: 'normal' } },
          } }),
        })
        if (response.ok) sent++
        else {
          failed++
          const error = await response.json().catch(() => ({}))
          if (error.error?.details?.some((detail: { errorCode?: string }) => detail.errorCode === 'UNREGISTERED')) {
            await db.from('crest_push_devices').delete().eq('id', device.id)
          }
        }
      }
    }
    const saved = await db.from('crest_notification_events').update({ status: failed ? 'partial' : 'complete', sent_count: sent, failed_count: failed }).eq('id', eventId)
    if (saved.error) throw saved.error
    return { sent, failed }
  } catch (error) {
    await db.from('crest_notification_events').update({ status: 'failed', sent_count: sent, failed_count: failed }).eq('id', eventId)
    throw error
  }
}

Deno.serve(request => serve(request, 'CREST_NOTIFICATIONS_ENABLED', notifications))
