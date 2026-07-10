package com.gamespacepro.feature.shizuku.service

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Raw outcome of running a command in [ShellCommandUserService], as it
 * crosses the AIDL/Binder boundary back to the client process.
 *
 * Kept separate from the domain's [com.gamespacepro.domain.shell.ShellCommandResult]
 * on purpose — this class is dictated by what AIDL/Parcelable can carry,
 * not by what's a clean domain model. [toDomainResult] does the mapping.
 */
@Parcelize
data class ShellCommandResultParcel(
    val isSuccess: Boolean,
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val executionTimeMillis: Long,
    val errorMessage: String?,
) : Parcelable
