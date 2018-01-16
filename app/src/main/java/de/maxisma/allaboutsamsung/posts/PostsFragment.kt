package de.maxisma.allaboutsamsung.posts

import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.query.Query
import de.maxisma.allaboutsamsung.query.newExecutor
import de.maxisma.allaboutsamsung.rest.wordpressApi
import de.maxisma.allaboutsamsung.utils.dpToPx
import kotlinx.android.synthetic.main.fragment_posts.*

class PostsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO Inject this
        val db = Room.databaseBuilder(context!!, Db::class.java, "db").build()

        val query = Query.Empty
        val executor = query.newExecutor(wordpressApi, db)

        val adapter = PostsAdapter()
        val lm = LinearLayoutManager(context!!)
        postList.adapter = adapter
        postList.layoutManager = lm
        postList.addItemDecoration(SpacingItemDecoration(horizontalSpacing = 8.dpToPx(), verticalSpacing = 8.dpToPx()))

        executor.data.observe(this, Observer<List<Post>> { it ->
            adapter.posts = it ?: emptyList()
            adapter.notifyDataSetChanged()
        }) // TODO Helper method
        executor.requestNewerPosts()
    }
}