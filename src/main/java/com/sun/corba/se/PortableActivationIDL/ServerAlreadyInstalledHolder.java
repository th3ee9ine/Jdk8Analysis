package com.sun.corba.se.PortableActivationIDL;

/**
* com/sun/corba/se/PortableActivationIDL/ServerAlreadyInstalledHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /scratch/jenkins/workspace/8-2-build-glinux-aarch64/jdk8u321/2141/corba/src/share/classes/com/sun/corba/se/PortableActivationIDL/activation.idl
* Wednesday, December 15, 2021 7:30:47 PM GMT
*/

public final class ServerAlreadyInstalledHolder implements org.omg.CORBA.portable.Streamable
{
  public com.sun.corba.se.PortableActivationIDL.ServerAlreadyInstalled value = null;

  public ServerAlreadyInstalledHolder ()
  {
  }

  public ServerAlreadyInstalledHolder (com.sun.corba.se.PortableActivationIDL.ServerAlreadyInstalled initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = com.sun.corba.se.PortableActivationIDL.ServerAlreadyInstalledHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    com.sun.corba.se.PortableActivationIDL.ServerAlreadyInstalledHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return com.sun.corba.se.PortableActivationIDL.ServerAlreadyInstalledHelper.type ();
  }

}
