package de.maxisma.allaboutsamsung.posts

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.PostId
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.wordpressApi
import de.maxisma.allaboutsamsung.utils.dpToPx
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.fragment_posts.*
import javax.inject.Inject

class PostsFragment : Fragment() {

    interface InteractionListener {
        fun displayPost(postId: PostId)
    }

    @Inject
    lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val query = Query.Empty
        val executor = query.newExecutor(wordpressApi, db)

        val adapter = PostsAdapter {
            (activity as InteractionListener).displayPost(it.id) // TODO Make property for listener in superclass
        }
        val lm = LinearLayoutManager(context!!)
        postList.adapter = adapter
        postList.layoutManager = lm
        postList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        executor.data.observe(this) { it ->
            adapter.posts = it ?: emptyList()
            adapter.notifyDataSetChanged()
        }
        executor.requestNewerPosts()
    }
}