package com.hahmetbuyukbesnili.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.hahmetbuyukbesnili.artbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    Intent intentToGallery;
    Intent intentFromResult;
    String name;
    String artistName;
    String year;
    byte[] byteArray;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        registerLauncher();
        intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        db = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.equals("fromMenu")) {

        }else{
            int artId = intent.getIntExtra("artId", 0);
            binding.button.setVisibility(View.INVISIBLE);
            binding.nameText.setEnabled(false);
            binding.artistText.setEnabled(false);
            binding.yearText.setEnabled(false);
            binding.imageView.setEnabled(false);
            try {
                Cursor cursor = db.rawQuery("SELECT * FROM arts WHERE id = ?", new String[] {String.valueOf(artId)});

                int artNameIx = cursor.getColumnIndex("name");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()) {
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(artistNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                } cursor.close();

            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public  void selectImage(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            } else {
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            //intent to gallery
            activityResultLauncher.launch(intentToGallery);
        }
    }

    public void save(View view) {
        name = binding.nameText.getText().toString();
        artistName = binding.artistText.getText().toString();
        year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byteArray = outputStream.toByteArray();

        if (binding.nameText.getText().toString().matches("") || binding.artistText.getText().toString().matches("")
                || binding.yearText.getText().toString().matches("")) {
            Toast.makeText(ArtActivity.this, "Fill in the empty slots!", Toast.LENGTH_LONG).show();
        } else {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, name VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)");
                String sqlString = "INSERT INTO arts(name, artistname, year, image) VALUES(?,?,?,?)";
                SQLiteStatement sqLiteStatement = db.compileStatement(sqlString);
                sqLiteStatement.bindString(1,name);
                sqLiteStatement.bindString(2,artistName);
                sqLiteStatement.bindString(3,year);
                sqLiteStatement.bindBlob(4,byteArray);
                sqLiteStatement.execute();

            }catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(ArtActivity.this,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void registerLauncher() {

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    intentFromResult = result.getData();
                    if (intentFromResult != null) {
                        Uri imageData = intentFromResult.getData();

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                            }else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                            }
                            binding.imageView.setImageBitmap(selectedImage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    //Intent to gallery
                    activityResultLauncher.launch(intentToGallery);

                } else {
                    Toast.makeText(ArtActivity.this,"Permission Needed!",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            width = maximumSize;
            height =(int) (width / bitmapRatio);
        } else {
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true);

    }

}