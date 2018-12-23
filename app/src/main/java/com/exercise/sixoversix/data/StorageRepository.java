package com.exercise.sixoversix.data;

import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.inject.Inject;

public class StorageRepository {

    @Inject
    StorageRepository() {
    }

    public void saveFile(final ImageReader reader, Handler backgroundHandler) {
        //new File(getActivity().getExternalFilesDir(null), "pic.jpg");
        //File file = new File(Environment.getExternalStorageDirectory()+"/"+ "pic"/*UUID.randomUUID().toString()*/+".jpg");
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "6Over6");
        ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = null;
                try{
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    save(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    {
                        if(image != null)
                            image.close();
                    }
                }
            }
            private void save(byte[] bytes) throws IOException {
                OutputStream outputStream = null;
                try{
                    outputStream = new FileOutputStream(file);
                    outputStream.write(bytes);
                }finally {
                    if(outputStream != null)
                        outputStream.close();
                }
            }
        };

        reader.setOnImageAvailableListener(readerListener, backgroundHandler);
    }
}
