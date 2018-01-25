package de.maxisma.allaboutsamsung.categories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.maxisma.allaboutsamsung.BaseActivity
import de.maxisma.allaboutsamsung.R
import de.maxisma.allaboutsamsung.app
import de.maxisma.allaboutsamsung.db.Category
import de.maxisma.allaboutsamsung.db.CategoryId
import de.maxisma.allaboutsamsung.db.Db
import de.maxisma.allaboutsamsung.utils.observe
import kotlinx.android.synthetic.main.activity_category.*
import javax.inject.Inject

private const val RESULT_CATEGORY_ID = "category_id"
private const val RESULT_CATEGORY_ID_DEFAULT = -1

fun newCategoryActivityIntent(context: Context) = Intent(context, CategoryActivity::class.java)

val Intent.categoryActivityResult: CategoryActivity.Result
    get() {
        val id = getIntExtra(RESULT_CATEGORY_ID, RESULT_CATEGORY_ID_DEFAULT)
        return CategoryActivity.Result(if (id != -1) id else null)
    }

// TODO Make prettier
// TODO Add synthetic category "all"?
class CategoryActivity : BaseActivity(useDefaultMenu = false) {

    data class Result(val categoryId: CategoryId?)

    @Inject
    lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)
        app.appComponent.inject(this)

        setResultInternal(null)

        categoryList.layoutManager = LinearLayoutManager(this)

        db.categoryDao.categories().observe(this) {
            it ?: return@observe

            categoryList.adapter = CategoryAdapter(it, onClick = {
                setResultInternal(it.id)
                finish()
            })
        }
    }

    private fun setResultInternal(categoryId: CategoryId?) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(RESULT_CATEGORY_ID, categoryId ?: RESULT_CATEGORY_ID_DEFAULT) })
    }
}

private class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val categoryDescription: TextView = itemView.findViewById(R.id.categoryDescription)
}

private class CategoryAdapter(private val categories: List<Category>, private val onClick: (Category) -> Unit) : RecyclerView.Adapter<CategoryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.categoryDescription.text = categories[position].name
        holder.itemView.setOnClickListener { onClick(categories[position]) }
    }

}