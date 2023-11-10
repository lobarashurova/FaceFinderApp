package uz.mlsoft.facefinderapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast

fun myLog(message: String) {
    Log.d("TTT", message)
}

fun myLog2(message: String) {
    Log.d("DDD", message)
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
