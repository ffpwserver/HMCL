/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2013  huangyuhui <huanghongxun2008@126.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hellominecraft.launcher.core.install.forge;

import org.jackhuang.hellominecraft.launcher.core.install.InstallProfile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jackhuang.hellominecraft.util.C;
import org.jackhuang.hellominecraft.util.logging.HMCLog;
import org.jackhuang.hellominecraft.launcher.core.service.IMinecraftService;
import org.jackhuang.hellominecraft.util.tasks.Task;
import org.jackhuang.hellominecraft.util.system.FileUtils;
import org.jackhuang.hellominecraft.util.NetUtils;
import org.jackhuang.hellominecraft.launcher.core.version.MinecraftLibrary;
import org.jackhuang.hellominecraft.util.MessageBox;

/**
 *
 * @author huangyuhui
 */
public class ForgeInstaller extends Task {

    public File gameDir;
    public File forgeInstaller;
    public IMinecraftService mp;

    public ForgeInstaller(IMinecraftService mp, File forgeInstaller) {
        this.gameDir = mp.baseDirectory();
        this.forgeInstaller = forgeInstaller;
        this.mp = mp;
    }

    @Override
    public void executeTask() throws Exception {
        HMCLog.log("Extracting install profiles...");

        try (ZipFile zipFile = new ZipFile(forgeInstaller)) {
            ZipEntry entry = zipFile.getEntry("install_profile.json");
            String content = NetUtils.getStreamContent(zipFile.getInputStream(entry));
            InstallProfile profile = C.GSON.fromJson(content, InstallProfile.class);
            File from = new File(gameDir, "versions" + File.separator + profile.install.getMinecraft());
            if (!from.exists())
                if (MessageBox.Show(C.i18n("install.no_version_if_intall")) == MessageBox.YES_OPTION) {
                    if (!mp.version().install(profile.install.getMinecraft(), null))
                        throw new IllegalStateException(C.i18n("install.no_version"));
                } else
                    throw new IllegalStateException(C.i18n("install.no_version"));
            File to = new File(gameDir, "versions" + File.separator + profile.install.getTarget());
            if (!to.exists() && !to.mkdirs())
                HMCLog.warn("Failed to make new version folder " + to);
            HMCLog.log("Copying jar..." + profile.install.getMinecraft() + ".jar to " + profile.install.getTarget() + ".jar");
            FileUtils.copyFile(new File(from, profile.install.getMinecraft() + ".jar"),
                               new File(to, profile.install.getTarget() + ".jar"));
            HMCLog.log("Creating new version profile..." + profile.install.getTarget() + ".json");
            FileUtils.write(new File(to, profile.install.getTarget() + ".json"), C.GSON.toJson(profile.versionInfo));
            HMCLog.log("Extracting universal forge pack..." + profile.install.getFilePath());
            entry = zipFile.getEntry(profile.install.getFilePath());
            InputStream is = zipFile.getInputStream(entry);
            MinecraftLibrary forge = new MinecraftLibrary(profile.install.getPath());
            File file = new File(gameDir, "libraries/" + forge.getDownloadInfo().path);
            if (file.getParentFile().mkdirs())
                HMCLog.warn("Failed to make library directory " + file.getParent());
            try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                int c;
                while ((c = is.read()) != -1)
                    bos.write((byte) c);
            }
            mp.version().refreshVersions();
        }
    }

    @Override
    public String getInfo() {
        return C.i18n("install.forge.install");
    }

}
