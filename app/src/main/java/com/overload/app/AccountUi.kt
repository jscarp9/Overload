package com.overload.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Settings card: create an account / sign in, or sync status and account actions. */
@Composable
fun AccountCard(vm: OverloadViewModel, d: AppData, confirm: String?) {
    val a = d.settings.acct; val s by vm.sync.status.collectAsState(); val days by vm.sync.cloudDays.collectAsState()
    OvCard {
        if (a == null) {
            Title("☁️ Back up and sync", 20)
            Sub("Keep every workout safe in the cloud, use Overload on more than one device (including the web app), and keep your crew spot if you change phones. Optional and free.", Modifier.padding(vertical = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OvButton("Create account", { vm.sync.create() }, bg = Red, fg = Color.White); OvButton("I have an account", { vm.openSheet(Sheet.SignIn) })
            }
            return@OvCard
        }
        LaunchedEffect(a.id) { vm.sync.loadCloudDays() }
        Title("☁️ Account", 20)
        Sub(when (s.state) { "err" -> "⚠️ Last sync failed. It will retry on its own"; "pending" -> "Syncing…"; "held" -> "Sync paused. Check the message about missing workouts"; else -> "Synced ✓" } +
            if (s.at > 0) " · ${ago(s.at)} ago" else "", Modifier.padding(vertical = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("Sync now", { vm.sync.now(true) { vm.toast(if (vm.sync.status.value.state == "ok") "Synced ✓" else "Sync failed. Will retry") } }, small = true)
            OvButton(if (confirm == "acout") "Tap again" else "Sign out", {
                if (confirm != "acout") { vm.setConfirm("acout"); vm.toast("Tap Sign out again. Your data stays on this phone and in the cloud") } else { vm.setConfirm(null); vm.sync.signOut() } }, small = true)
        }
        Sub("Passkeys made on the web app work there. On Android, sign in with your recovery code.", Modifier.padding(top = 8.dp))
        days?.let { list -> if (list.isNotEmpty()) {
            OvText("CLOUD · LAST 30 DAYS", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 12.dp, bottom = 6.dp))
            list.reversed().chunked(3).forEach { row -> Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { day -> val k = "c$day"; val dd = toD(day)
                    OvButton(if (confirm == k) "Tap again to restore" else "${DN[dayIdx(dd)]} ${md(dd)}", { if (confirm != k) vm.setConfirm(k) else { vm.setConfirm(null); vm.sync.restoreDay(day) } }, small = true) }
            } }
        } }
        OvButton(if (confirm == "acdel") "Tap again. This deletes your cloud copy" else "Delete my account and cloud data",
            { if (confirm != "acdel") vm.setConfirm("acdel") else { vm.setConfirm(null); vm.sync.deleteAccount() } }, Modifier.padding(top = 10.dp), fg = Red, wide = true)
    }
}

@Composable
fun SignInSheet(vm: OverloadViewModel) {
    var code by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    SheetHead("Sign in", "Cancel") { vm.closeSheet() }
    OvText("USE YOUR RECOVERY CODE", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(bottom = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextBox(code, { code = it.uppercase() }, "ABCD-EFGH-JKLM-NPQR", Modifier.weight(1f))
        OvButton(if (busy) "…" else "Go", { if (!busy) { busy = true; vm.sync.recover(code) { busy = false } } }, bg = Red, fg = Color.White)
    }
    Sub("Workouts already on this phone get merged with your account. Nothing is deleted.", Modifier.padding(top = 12.dp))
    Sub("Made your account with a passkey on the web app? Open Overload on the web, go to Settings → Back up and sync, and use the recovery code you saved then.", Modifier.padding(top = 8.dp))
}

@Composable
fun RecoveryCodeSheet(vm: OverloadViewModel, code: String) {
    val ctx = LocalContext.current
    Title("Save your recovery code", 24)
    Sub("This code is the master key to your account. Save it somewhere safe, like your notes app or a screenshot. It can't be shown again.", Modifier.padding(vertical = 8.dp))
    Column(Modifier.fillMaxWidth().background(ov.soft, RoundedCornerShape(18.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Title(code, 24)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("Copy", { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Overload recovery code", code)); vm.toast("Copied") }, small = true)
            OvButton("Share", { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "My Overload recovery code: $code"), null)) }, small = true)
        }
    }
    OvButton("I saved it", { vm.closeSheet(); vm.toast("Backup and sync is on ☁️") }, Modifier.padding(top = 12.dp), bg = Red, fg = Color.White, wide = true)
}

@Composable
fun SyncGuardSheet(vm: OverloadViewModel, n: Int) {
    Title("Workouts missing", 24)
    Sub("$n workouts are gone from this phone but still safe in your account. Did you delete them on purpose?", Modifier.padding(vertical = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OvButton("Bring them back", { vm.sync.bringBack() }, Modifier.weight(1f), bg = Red, fg = Color.White)
        OvButton("Delete them", { vm.sync.deleteMissing() }, Modifier.weight(1f))
    }
}
