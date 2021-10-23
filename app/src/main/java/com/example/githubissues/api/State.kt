package com.example.githubissues.api

// Issue state
enum class State(val state: String) {
    OPEN("open"),
    CLOSED("closed"),
    ALL("all")
}
