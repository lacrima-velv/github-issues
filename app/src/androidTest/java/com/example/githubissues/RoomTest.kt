package com.example.githubissues

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.githubissues.db.IssuesDao
import com.example.githubissues.db.IssuesDatabase
import com.example.githubissues.db.RemoteKeys
import com.example.githubissues.db.RemoteKeysDao
import com.example.githubissues.model.Issue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomTest {
    private lateinit var issueDao: IssuesDao
    private lateinit var remoteKeysDao: RemoteKeysDao
    private lateinit var database: IssuesDatabase

    private val unselectedIssue = Issue(
        id = 1,
        user = Issue.User(1, "userLogin","ava_url"),
        assignee = Issue.Assignee(1, "assigneeLogin", "ava_url"),
        number = 1,
        title = "Title",
        state = "Open",
        body = "Body",
        createdAt = "Date",
        updatedAt = "Date",
        closedAt = "Date",
        isSelected = 0
    )

    private val selectedIssue = Issue(
        id = 2,
        user = Issue.User(1, "userLogin2","ava_url2"),
        assignee = Issue.Assignee(1, "assigneeLogin2", "ava_url2"),
        number = 1,
        title = "Title",
        state = "Open",
        body = "Body",
        createdAt = "Date",
        updatedAt = "Date",
        closedAt = "Date",
        isSelected = 1
    )

    private val remoteKeyOfUnselectedIssue = RemoteKeys(
        issueId = 1,
        prevKey = 1,
        nextKey = 2
    )

    private val remoteKeyOfSelectedIssue = RemoteKeys(
        issueId = 2,
        prevKey = 3,
        nextKey = 4
    )

    @Before
    fun createDatabase() {
        //val context = ApplicationProvider.getApplicationContext<Context>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context, IssuesDatabase::class.java).build()
        issueDao = database.issuesDao()
        remoteKeysDao = database.remoteKeysDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    @Throws(Exception::class)
    fun getIssueDetailsByIdTest() {
        runBlocking {
            issueDao.insertAll(listOf(unselectedIssue))
            val byId = issueDao.getIssueDetailsById(1)
            Assert.assertEquals(1, byId.id)
        }
    }

    @Test
    @Throws(Exception::class)
    fun deleteRemoteKeysOfUnselectedIssues() {
        runBlocking {
            issueDao.insertAll(listOf(unselectedIssue, selectedIssue))
            remoteKeysDao.insertAll(listOf(remoteKeyOfSelectedIssue, remoteKeyOfUnselectedIssue))
            remoteKeysDao.clearRemoteKeys()
            val idsOfSelectedIssues = remoteKeysDao.getRemoteKeysByIssueId(2)
            val idsOfUnselectedIssues = remoteKeysDao.getRemoteKeysByIssueId(1)

            // Check if there are selected issues in remoteKeys table, if we deleted just unselected
            Assert.assertEquals(2L, idsOfSelectedIssues?.issueId)
            // Check if there are unselected issues in remoteKeys table, if we deleted unselected
            Assert.assertEquals(null, idsOfUnselectedIssues?.issueId)
        }
    }
}