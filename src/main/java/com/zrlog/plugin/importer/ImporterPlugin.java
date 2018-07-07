package com.zrlog.plugin.importer;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginAction;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;

public class ImporterPlugin implements IPluginAction {
    @Override
    public void start(IOSession ioSession, MsgPacket msgPacket) {

    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {

    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {

    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {

    }
}
