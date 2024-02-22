package com.example.notes.Database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize()
@Entity("Note")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("content")
    val content: String,
    @ColumnInfo("date")
    val date: String,
    @ColumnInfo("isSelected")
    var isSelected: Int = 0

) : Parcelable {
    constructor() : this(0, "dummy text", "dummy text", "dummy text", 0)

}
