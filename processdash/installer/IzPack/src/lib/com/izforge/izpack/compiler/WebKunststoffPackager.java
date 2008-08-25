/*
 *  $Id$
 *  IzPack
 *  Copyright (C) 2001-2003 Julien Ponge
 *
 *  File :               WebKunststoffPackager.java
 *  Description :        The web installer packager class with the Kunststoff L&F support.
 *  Author's email :     julien@izforge.com
 *  Author's Website :   http://www.izforge.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.izforge.izpack.compiler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *  Web installer class with the Kunststoff L&F support.
 *
 * @author     Julien Ponge
 */
public class WebKunststoffPackager extends WebPackager
{

    /**
     *  The constructor.
     *
     * @param  outputFilename  The output filename.
     * @param  plistener       The packager listener.
     * @exception  Exception   Description of the Exception
     */
    public WebKunststoffPackager(String outputFilename, PackagerListener plistener)
        throws Exception
    {
        super(outputFilename, plistener);

        // Copies the Kunststoff library
        sendMsg("Copying the Kunststoff library ...");
        ZipInputStream skeleton_is = null;
        InputStream rawInput = getClass().getResourceAsStream("/lib/kunststoff.jar");

        if (rawInput == null)
        {
            skeleton_is = new JarInputStream (new FileInputStream (
                Compiler.IZPACK_HOME + "lib" + File.separator + "kunststoff.jar"));
        }
        else
        {
            skeleton_is = new ZipInputStream (rawInput);
        }

        ZipEntry zentry;

        while ((zentry = skeleton_is.getNextEntry()) != null)
        {
            if (zentry.isDirectory())
                continue;

            // Puts a new entry
            outJar.putNextEntry(new ZipEntry(zentry.getName()));

            // Copy the data
            copyStream(skeleton_is, outJar);

            outJar.closeEntry();
            skeleton_is.closeEntry();
        }

    }


    /**
     *  Tells the packager to finish the job (misc writings, cleanups, closings ,
     *  ...).
     *
     * @exception  Exception  Description of the Exception
     */
    public void finish() throws Exception
    {
        // Usefull stuff
        DataOutputStream datOut;
        ObjectOutputStream objOut;
        int size;
        int i;

        sendMsg("Finishing the enpacking ...");

        // Writes the installation kind information
        outJar.putNextEntry(new ZipEntry("kind"));
        datOut = new DataOutputStream(outJar);
        datOut.writeUTF("web-kunststoff");
        datOut.flush();
        outJar.closeEntry();

        // Writes the packs informations
        outJar.putNextEntry(new ZipEntry("packs.info"));
        objOut = new ObjectOutputStream(outJar);
        size = packs.size();
        objOut.writeInt(size);
        for (i = 0; i < size; i++)
            objOut.writeObject(packs.get(i));
        objOut.flush();
        outJar.closeEntry();

        // Writes the langpacks informations
        outJar.putNextEntry(new ZipEntry("langpacks.info"));
        datOut = new DataOutputStream(outJar);
        size = langpacks.size();
        datOut.writeInt(size);
        for (i = 0; i < size; i++)
            datOut.writeUTF((String) langpacks.get(i));
        datOut.flush();
        outJar.closeEntry();

        // Closes the stream
        outJar.flush();
        outJar.close();

        sendStop();
    }
}
