package de.maxisma.allaboutsamsung.posts

import de.maxisma.allaboutsamsung.db.Post

data class PostViewModel(val post: Post, val isBreaking: Boolean)