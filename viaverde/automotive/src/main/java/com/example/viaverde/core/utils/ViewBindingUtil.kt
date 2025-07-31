package com.example.viaverde.core.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * Utility class for ViewBinding operations
 */
object ViewBindingUtil {

    /**
     * Inflate ViewBinding for a class
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified VB : ViewBinding> inflate(
        inflater: LayoutInflater,
        parent: ViewGroup? = null,
        attachToParent: Boolean = false
    ): VB {
        val vbClass = (VB::class.java.genericSuperclass as? ParameterizedType)
            ?.actualTypeArguments?.get(0) as? Class<VB>
            ?: throw IllegalArgumentException("Could not determine ViewBinding class")

        val inflateMethod = vbClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return inflateMethod.invoke(null, inflater, parent, attachToParent) as VB
    }

    /**
     * Bind ViewBinding to a view
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified VB : ViewBinding> bind(view: View): VB {
        val vbClass = (VB::class.java.genericSuperclass as? ParameterizedType)
            ?.actualTypeArguments?.get(0) as? Class<VB>
            ?: throw IllegalArgumentException("Could not determine ViewBinding class")

        val bindMethod = vbClass.getMethod("bind", View::class.java)
        return bindMethod.invoke(null, view) as VB
    }
}
