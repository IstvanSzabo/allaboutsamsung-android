package de.maxisma.allaboutsamsung.posts

import de.maxisma.allaboutsamsung.db.Post
import de.maxisma.allaboutsamsung.utils.StyledTitle

data class PostViewModel(val post: Post, val isBreaking: Boolean, val styledString: StyledTitle)