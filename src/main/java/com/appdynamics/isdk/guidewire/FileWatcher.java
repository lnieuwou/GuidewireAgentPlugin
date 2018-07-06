package com.appdynamics.isdk.guidewire;

import java.io.File;
// import java.io.IOException;
// import java.nio.file.*;
// import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by louis.nieuwoudt on 13/06/2018.
 * https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
 */
public class FileWatcher extends Thread {

        private final File file;
        private AtomicBoolean stop = new AtomicBoolean(false);
        private FileChangeHandler fileChangeHandler ;

        public FileWatcher(File file, FileChangeHandler fc )
        {
            this.file = file;
            this.fileChangeHandler = fc ;
        }

        public boolean isStopped() { return stop.get(); }
        public void stopThread() { stop.set(true); }

        public void doOnChange() {
            fileChangeHandler.onFileChanged();
        }

        @Override
        public void run()
        {
            // TODO : Fix file watcher for Java 1.6
            /*
            WatchService watcher = null;
            try {
                watcher = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                return ;
            }

            try  {
                Path path = file.toPath().getParent();
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                while (!isStopped())
                {
                    WatchKey key;
                    // Slow watch
                    try { key = watcher.poll(30, TimeUnit.SECONDS); }
                    catch (InterruptedException e) { return; }
                    if (key == null) { Thread.yield(); continue; }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            Thread.yield();
                            continue;
                        } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                                && filename.toString().equals(file.getName())) {
                            doOnChange();
                        }
                        boolean valid = key.reset();
                        if (!valid) { break; }
                    }
                    Thread.yield();
                }
            } catch (Throwable e) {
                // Log or rethrow the error
            }

            try {
                watcher.close() ;
            } catch (IOException e) {
                return ;
            }
            */
        }
}
