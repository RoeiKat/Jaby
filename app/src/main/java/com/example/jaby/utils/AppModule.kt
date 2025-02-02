package com.example.jaby.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.google.firebase.database.DatabaseReference
import dagger.Provides

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideContext(@ApplicationContext context: Context) : Context = context.applicationContext

    @Provides
    fun provideGson():Gson = Gson()

    @Provides
    fun provideDataBaseInstance():FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    fun provideDatabaseReference(db:FirebaseDatabase): DatabaseReference = db.reference
}