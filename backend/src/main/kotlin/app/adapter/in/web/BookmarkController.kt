package app.adapter.`in`.web

import app.domain.port.`in`.BookmarkUseCase
import app.domain.port.`in`.BookmarkView
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 책갈피(FR-011b): 설정(PUT)·해제(DELETE)·목록(GET). */
@RestController
class BookmarkController(private val bookmarks: BookmarkUseCase) {

    @PutMapping("/editions/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(@CurrentUserId userId: Long, @PathVariable id: Long) = bookmarks.setBookmark(userId, id, true)

    @DeleteMapping("/editions/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@CurrentUserId userId: Long, @PathVariable id: Long) = bookmarks.setBookmark(userId, id, false)

    @GetMapping("/bookmarks")
    fun list(@CurrentUserId userId: Long): List<BookmarkView> = bookmarks.list(userId)
}
