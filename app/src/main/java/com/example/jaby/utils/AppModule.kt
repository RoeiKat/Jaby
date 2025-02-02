package com.example.jaby.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.google.firebase.database.DatabaseReference

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    fun provideContext(@ApplicationContext context: Context) : Context = context.applicationContext

    fun provideGson():Gson = Gson()

    fun provideDataBaseInstance():FirebaseDatabase = FirebaseDatabase.getInstance()

    fun provideDatabaseReference(db:FirebaseDatabase): DatabaseReference = db.reference
}