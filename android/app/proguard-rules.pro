# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# From StudentHomeViewModel.kt & GroupDetailViewModel.kt
-keep class com.bdbshs.crest.ui.viewmodels.StudentDetails { <init>(); *; }
-keep class com.bdbshs.crest.ui.viewmodels.GroupDetails { <init>(); *; }

# From ResearchesViewModel.kt
-keep class com.bdbshs.crest.ui.viewmodels.ResearchItem { <init>(); *; }

# From GroupsViewModel.kt
-keep class com.bdbshs.crest.ui.viewmodels.GroupItem { <init>(); *; }

# From AccountsViewModel.kt (if they are separate data classes)
# If AccountItem is a sealed class, you need to keep its subclasses too.
-keep class com.bdbshs.crest.ui.viewmodels.AccountItem
-keep class com.bdbshs.crest.ui.viewmodels.AccountItem$Student { <init>(); *; }
-keep class com.bdbshs.crest.ui.viewmodels.AccountItem$Teacher { <init>(); *; }

# Keep the sealed class and its subclasses
-keep class com.bdbshs.crest.ui.viewmodels.UserDetails

# Keep all subclasses of UserDetails and their members
-keepclassmembers class com.bdbshs.crest.ui.viewmodels.UserDetails$Student {
    <init>(...);
    <fields>;
}

-keepclassmembers class com.bdbshs.crest.ui.viewmodels.UserDetails$Teacher {
    <init>(...);
    <fields>;
}

# Keep the actual classes as well
-keep class com.bdbshs.crest.ui.viewmodels.UserDetails$Student
-keep class com.bdbshs.crest.ui.viewmodels.UserDetails$Teacher

-keep class com.bdbshs.crest.ui.viewmodels.DocumentItem{<init>(); *;}

-keep class com.bdbshs.crest.ui.viewmodels.DashboardCardItem{<init>(); *;}
-keep class com.bdbshs.crest.ui.viewmodels.SimpleResearch{<init>(); *;}

# Firebase and Google Play Services rules are often included automatically,
# but it's good practice to ensure they are present.
# (These might already be in your gradle files or applied by plugins)
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.firebase.** { *; }

# Appwrite SDK rules (check their documentation for official rules)
# A general rule would be to keep their model classes if you use them with serialization.
-keep class io.appwrite.models.** { *; }
# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile