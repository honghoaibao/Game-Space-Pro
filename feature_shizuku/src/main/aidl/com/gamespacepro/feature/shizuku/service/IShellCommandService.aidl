package com.gamespacepro.feature.shizuku.service;

import com.gamespacepro.feature.shizuku.service.ShellCommandResultParcel;

interface IShellCommandService {

    /** Runs [command] to completion and returns the raw outcome. Blocking. */
    ShellCommandResultParcel runCommand(in String[] command);

    /**
     * Explicit client-requested shutdown of this service process. Not
     * currently called automatically anywhere — see feature_shizuku/README.md.
     */
    void destroy();
}
