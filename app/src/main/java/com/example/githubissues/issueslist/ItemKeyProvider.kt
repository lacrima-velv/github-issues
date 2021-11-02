package com.example.githubissues.issueslist

import androidx.recyclerview.selection.ItemKeyProvider


//class MyItemKeyProvider(private val adapter: IssuesPagingAdapter): ItemKeyProvider<Long>(SCOPE_CACHED) {
//    override fun getKey(position: Int): Long? {
//        return adapter.snapshot().items[position].id
//    }
//
//    override fun getPosition(key: Long): Int {
//        return adapter.snapshot().items.indexOfFirst { it.id == key }
//    }
//
//}