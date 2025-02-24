package com.example.jaby.utils

private const val VALID_TIME = 60000

enum class DataModelType {
    StartWatching,Offer,Answer,IceCandidates,EndMonitoring,EndWatching,SwitchMonitorCamera
}

data class DataModel (
    val sender:String?=null,
    val target:String,
    val type:DataModelType,
    val data:String?=null,
    val timeStamp:Long = System.currentTimeMillis()
)

fun DataModel.isValid(): Boolean {
    return System.currentTimeMillis() - this.timeStamp < VALID_TIME
}
