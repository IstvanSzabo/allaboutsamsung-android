package de.maxisma.allaboutsamsung.post

import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.db.PostId

private const val EXTRA_POST_ID = "post_id"

fun newPostActivityIntent(context: Context, postId: PostId) = Intent(context, PostActivity::class.java).apply {
    putExtra(EXTRA_POST_ID, postId)
}

class PostActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = PostFragment(intent.getLongExtra(EXTRA_POST_ID, -1))
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()
    }
}