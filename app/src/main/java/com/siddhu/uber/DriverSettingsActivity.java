package com.siddhu.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField,mCarField;

    private Button mBack,mConfirm;

    private String userID;

    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;

    private ImageView mProfileImage;

    private String mName;
    private String mPhone;
    private String mCar;
    private String mProfileImageUrl;

    private Uri resultUri;

    private RadioGroup mRadioGroup;
    int servicetId;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        mNameField = findViewById(R.id.name);
        mPhoneField  = findViewById(R.id.phone);
        mCarField = findViewById(R.id.car);

        mBack = findViewById(R.id.back);
        mConfirm = findViewById(R.id.confirm);

        mAuth = FirebaseAuth.getInstance();

        mProfileImage = findViewById(R.id.profileImage);


        userID = mAuth.getCurrentUser().getUid();
        mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userID);

        mRadioGroup = findViewById(R.id.radioGroup);



        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

    private void getUserInfo(){
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String,Object> map = (Map <String,Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("car") != null){
                        mCar = map.get("car").toString();
                        mCarField .setText(mCar);
                    }
                    if(map.get("service") != null){
                        servicetId = Integer.valueOf( map.get("service").toString());
                        mRadioGroup.check(servicetId);
                    }
                    if(map.get("profileImageUrl") != null){
                        mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }



    private void saveUserInformation(){

        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();
        mCar = mCarField.getText().toString();
        servicetId = mRadioGroup.getCheckedRadioButtonId();

        final RadioButton mRadioButton = (RadioButton) findViewById(servicetId);



        Map userInfo = new HashMap();
        userInfo.put("name",mName);
        userInfo.put("phone",mPhone);
        userInfo.put("car",mCar);
        userInfo.put("service",String.valueOf(servicetId));
        mDriverDatabase.updateChildren(userInfo);

        if(resultUri !=null){

            final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            }catch (Exception e){
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
            }
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,boas);
            byte[] data = boas.toByteArray();

            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            });


            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();

                    firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String url = uri.toString();

                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", url);
                            mDriverDatabase.updateChildren(newImage);

                            finish();
                            return;

                        }
                    });
                }
            });
        }else {
            finish();
        }

        finish();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

    }
}
