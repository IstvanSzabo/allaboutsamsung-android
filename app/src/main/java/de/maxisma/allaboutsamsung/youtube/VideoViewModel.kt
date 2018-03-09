package de.maxisma.allaboutsamsung.youtube

import de.maxisma.allaboutsamsung.db.Video
import de.maxisma.allaboutsamsung.utils.StyledTitle

data class VideoViewModel(val video: Video, val styledTitle: StyledTitle)