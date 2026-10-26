import { admin, assert, findDocuments, identifier, member, paths, readDocument, serve, type User } from '../_shared/backend.ts'

async function references(fileId: string, alias: string | null) {
  const ids = alias ? [fileId, alias] : [fileId]
  const collections = [paths.documents, paths.qualitative, paths.quantitative, paths.groups]
  return (await Promise.all(collections.flatMap(path => ids.map(async id => ({
    path, records: await findDocuments(path, 'file_link', id),
  }))))).flatMap(({ path, records }) => records.map(record => ({ path, ...record })))
}

async function storage(body: Record<string, unknown>, user: User) {
  assert(user.teacher || user.student, 403, 'An approved school account is required.')
  const db = admin()
  const bucket = Deno.env.get('CREST_STORAGE_BUCKET') || 'crest-files'
  const bucketInfo = await db.storage.getBucket(bucket)
  assert(!bucketInfo.error && bucketInfo.data?.public === false, 503, 'School file storage must use a private bucket.')
  if (body.action === 'create-upload') {
    const kind = body.kind
    assert(['research', 'document', 'group'].includes(String(kind)), 400, 'Invalid upload type.')
    const size = body.size
    assert(typeof size === 'number' && Number.isInteger(size) && size > 0 && size <= 20 * 1024 * 1024, 400, 'Choose a PDF of up to 20 MB.')
    let groupId: string | null = null
    if (kind === 'group') {
      groupId = identifier(body.groupId)
      const group = await readDocument(`${paths.groups}/${groupId}`)
      assert(group && member(group.data, user.uid), 403, 'Only current group members can submit files.')
      assert(group.data.accepted_research !== true, 409, 'This submission has already been approved.')
    } else assert(user.teacher, 403, 'An approved teacher account is required.')
    const id = `sb_${crypto.randomUUID()}`
    const objectPath = `uploads/${id}.pdf`
    const { error } = await db.from('crest_storage_files').insert({
      id, bucket, object_path: objectPath, owner_uid: user.uid, kind, group_id: groupId, size_bytes: size,
    })
    if (error) throw error
    const signed = await db.storage.from(bucket).createSignedUploadUrl(objectPath, { upsert: false })
    if (signed.error) {
      await db.from('crest_storage_files').delete().eq('id', id)
      throw signed.error
    }
    return { fileId: id, url: signed.data.signedUrl }
  }

  assert(['read-file', 'delete-file'].includes(String(body.action)), 400, 'Unknown storage action.')
  const fileId = identifier(body.fileId)
  const result = await db.from('crest_storage_files').select('*')
    .eq(/^sb_[0-9a-f-]{36}$/.test(fileId) ? 'id' : 'legacy_file_id', fileId).maybeSingle()
  if (result.error) throw result.error
  assert(result.data, 404, 'This file has not been copied to Supabase yet.')
  const file = result.data
  const refs = await references(file.id, file.legacy_file_id)

  if (body.action === 'read-file') {
    const allowed = refs.some(ref => ref.path !== paths.groups || user.teacher ||
      (member(ref.data, user.uid) && file.group_id === ref.id))
    assert(allowed, 403, 'You do not have access to this file.')
    const signed = await db.storage.from(file.bucket).createSignedUrl(file.object_path, 60)
    if (signed.error) throw signed.error
    return { url: signed.data.signedUrl, expiresIn: 60 }
  }

  // Physical removal is deliberately excluded from the prepared integration.
  // Existing client deletion flows may detach metadata, but objects are retained.
  assert(user.teacher || file.owner_uid === user.uid, 403, 'You cannot remove this file.')
  return { deleted: false, retained: true }
}

// Initialization and credentials remain lazy while the feature flag is false.
Deno.serve(request => serve(request, 'CREST_STORAGE_ENABLED', storage))
