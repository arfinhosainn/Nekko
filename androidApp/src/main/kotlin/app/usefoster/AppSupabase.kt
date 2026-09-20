package app.usefoster

import app.usefoster.onboarding.data.supabase.createAppSupabaseClient
import io.github.jan.supabase.SupabaseClient

/**
 * THE app-wide Supabase client.
 *
 * The app (MainActivity), onboarding, and the home widget all share this ONE
 * instance. Sharing matters: the Auth plugin owns the persisted session and
 * its refresh lifecycle — two clients would race each other's
 * `autoSaveToStorage` writes and could silently invalidate the session.
 */
object AppSupabase {
    val client: SupabaseClient by lazy { createAppSupabaseClient() }
}