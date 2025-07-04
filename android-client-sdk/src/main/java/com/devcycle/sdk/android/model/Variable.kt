/*
 * DevCycle Client SDK API
 * Documents the DevCycle Client SDK API which powers bucketing and descisions for DevCycle's client SDKs.
 *
 * OpenAPI spec version: 1-oas3
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package com.devcycle.sdk.android.model

import com.devcycle.sdk.android.api.DevCycleCallback
import com.devcycle.sdk.android.listener.BucketedUserConfigListener
import com.devcycle.sdk.android.exception.DVCVariableException
import com.devcycle.sdk.android.util.JSONMapper
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.json.JSONArray
import org.json.JSONObject
import com.devcycle.sdk.android.util.DevCycleLogger
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.IllegalArgumentException
import kotlin.Deprecated
import kotlin.ReplaceWith

/**
 * Variable
 */
class Variable<T> internal constructor(
    /**
     * unique database id
     * @return _id
     */
    @JsonProperty("_id")
    var id: String? = null,
    /**
     * Unique key by Project, can be used in the SDK / API to reference by 'key' rather than _id.
     * @return key
     */
    val key: String,
    /**
     * Variable type
     * @return type
     */
    val type: TypeEnum,

    /**
     * Variable value can be a string, number, boolean, or JSON
     * @return value
     */
    var value: T,

    @JsonIgnore
    val defaultValue: T,

    @JsonIgnore
    var isDefaulted: Boolean? = null
) : PropertyChangeListener {

    /**
     * Variable type
     */
    enum class TypeEnum(@get:JsonValue val value: String) {
        STRING("String"), BOOLEAN("Boolean"), NUMBER("Number"), JSON("JSON");
    }

    @JsonIgnore
    var eval: EvalReason? = null

    /**
     * Evaluation Reason
     * @deprecated Use eval instead
     * @return null
     */
    @JsonIgnore
    @Deprecated("Use eval instead", ReplaceWith("eval"))
    var evalReason: String? = null

    @JsonIgnore
    private var callback: DevCycleCallback<Variable<T>>? = null

    @JsonIgnore
    private val coroutineScope: CoroutineScope = MainScope()

    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    private fun updateVariable(variable: BaseConfigVariable) {
        if (getType(variable.value) != type) {
            throw DVCVariableException("Cannot update Variable with a different type", this as Variable<Any>, variable)
        }
        id = variable.id
        val executeCallBack = hasValueChanged(value, variable.value as T)

        value = variable.value as T

        isDefaulted = false
        eval = variable.eval

        if (executeCallBack) {
            val self = this
            coroutineScope.launch {
                callback?.onSuccess(self)
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    private fun defaultVariable() {
        val executeCallBack = hasValueChanged(value, defaultValue)
        isDefaulted = true
        eval = EvalReason.defaultReason("User Not Targeted")

        if (executeCallBack) {
            value = defaultValue
            val self = this
            coroutineScope.launch {
                callback?.onSuccess(self)
            }
        }
    }

    private fun hasValueChanged(oldValue: T, newValue: T): Boolean {
        if (newValue?.javaClass == JSONObject::class.java || newValue?.javaClass == JSONArray::class.java) {
            val new = JSONMapper.mapper.readTree(newValue.toString())
            val existing = JSONMapper.mapper.readTree(oldValue.toString())

            if (new != existing) {
                return true
            }
        } else if (newValue != oldValue) {
            return true
        }

        return false
    }

    companion object {
        @JvmSynthetic internal fun <T: Any> initializeFromVariable(key: String, defaultValue: T, readOnlyVariable: BaseConfigVariable?): Variable<T> {
            val type = getType(defaultValue)
            val configVariableType = readOnlyVariable?.let { getType(it.value) }
            if (readOnlyVariable != null && type != null && configVariableType === type) {
                @Suppress("UNCHECKED_CAST")
                val returnVariable = Variable(
                    id = readOnlyVariable.id,
                    key = key,
                    value = readOnlyVariable.value as T,
                    type = type,
                    defaultValue = defaultValue as T
                )
                returnVariable.eval = readOnlyVariable.eval
                returnVariable.isDefaulted = false
                return returnVariable
            } else {
                val returnVariable = Variable(
                    key = key,
                    value = defaultValue,
                    type = getAndValidateType(defaultValue),
                    defaultValue = defaultValue
                )
                returnVariable.isDefaulted = true

                if (readOnlyVariable != null && configVariableType !== type) {
                    returnVariable.eval = EvalReason.defaultReason("Variable Type Mismatch")
                    DevCycleLogger.e("Mismatched variable type for variable: $key, using default")
                } else {
                    returnVariable.eval = EvalReason.defaultReason( "User Not Targeted")
                }

                return returnVariable
            }
        }

        private fun <T: Any> getType(value: T, fromReadOnlyVariable: Boolean = false): TypeEnum? {
            val typeClass = value::class.java

            var typeEnum = when {
                // Kotlin types
                String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                JSONArray::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON

                // Java types
                java.lang.String::class.java.isAssignableFrom(typeClass) -> TypeEnum.STRING
                java.lang.Number::class.java.isAssignableFrom(typeClass) -> TypeEnum.NUMBER
                java.lang.Boolean::class.java.isAssignableFrom(typeClass) -> TypeEnum.BOOLEAN
                org.json.JSONObject::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                org.json.JSONArray::class.java.isAssignableFrom(typeClass) -> TypeEnum.JSON
                else -> null
            }

            return typeEnum
        }

        @JvmSynthetic internal fun <T: Any> getAndValidateType(defaultValue: T): TypeEnum {
            val type = getType(defaultValue)
            if (type != null) {
                return type
            }
            throw IllegalArgumentException("${defaultValue::class.java} is not a valid type. Must be String / Number / Boolean or JSONObject")
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == BucketedUserConfigListener.BucketedUserConfigObserverConstants.propertyChangeConfigUpdated) {
            val config = evt.newValue as BucketedUserConfig
            val variable: BaseConfigVariable? = config.variables?.get(key)
            DevCycleLogger.v("Triggering property change handler for $key")
            if (variable != null) {
                try {
                    updateVariable(variable)
                } catch (e: DVCVariableException) {
                    // Only logs the error and continues to use the existing Variable value & definition, no update is triggered
                    DevCycleLogger.e("Mismatched variable type for variable: ${variable.key}, using default")
                }
            } else {
                try {
                    defaultVariable()
                } catch (e: DVCVariableException) {
                    DevCycleLogger.e("Unable to restore variable to default")
                }
            }
        }
    }

    /**
     * To be notified when Variable.value changes register a callback by calling this method. The
     * callback will replace any previously registered callback.
     *
     * [callback] returns the updated Variable inside callback.onSuccess(..)
     */
    @Deprecated("Use the plain callback signature instead.")
    fun onUpdate(callback: DevCycleCallback<Variable<T>>) {
        this.callback = callback
    }

    /**
     * To be notified when Variable.value changes register a callback by calling this method. The
     * callback will replace any previously registered callback.
     *
     * [callback] called with the updated variable
     */
    fun onUpdate(callback: (Variable<T>) -> Unit) {
        this.callback = object: DevCycleCallback<Variable<T>> {
            override fun onSuccess(result: Variable<T>) {
                callback(result)
            }

            override fun onError(t: Throwable) {
                // no-op
            }
        }
    }
}