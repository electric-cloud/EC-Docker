
// VersionInfo.java --
//
// VersionInfo.java is part of ElectricCommander.
//
// Copyright (c) 2005-2016 Electric Cloud, Inc.
// All rights reserved.
//
package ecplugins.EC_Docker.client;
public class VersionInfo
{

    //~ Instance fields --------------------------------------------------------

    String m_version;
    boolean m_latest;

    //~ Constructors -----------------------------------------------------------

    public VersionInfo() { }

    public VersionInfo(String version, boolean latest) {
        m_version = version;
        m_latest = latest;
    }

    //~ Methods ----------------------------------------------------------------

    public String getVersion()
    {
        return m_version;
    }

    public void setVersion(String version)
    {
        this.m_version = version;
    }

    public boolean getLatest() {
        return m_latest;
    }

    public void setLatest(boolean latest) {
        this.m_latest = latest;
    }

    public String toString() {
        String retval = "";
        if (m_latest) {
            retval += "Latest is on";
        }
        else {
            retval += "Latest is off";
        }
        retval += "; version is " + m_version;
        return retval;
    }
}
