package com.umoji.umoji.Duel;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.SectionsPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class DuelActivity extends AppCompatActivity {
    private static final String TAG = "DuelActivity";
    private static final int DUEL_FRAGMENT = 1;

    private Context mContext = DuelActivity.this;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duel);
        Log.d(TAG, "onCreate: creating...");

        mViewPager = (ViewPager) findViewById(R.id.viewpager_container);

        setupBottomNavigationView();
        setupViewPager();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started...");

        mViewPager.setCurrentItem(DUEL_FRAGMENT);
    }

    private void setupViewPager(){
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new MatchingFragment()); //index 0
        adapter.addFragment(new DuelFragment()); //index 1
        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.duel_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setText("MATCHES");
        tabLayout.getTabAt(1).setText("ARENA");
    }

    private void setupBottomNavigationView(){
        ImageView homeBtn, homeBlueBtn, cameraBtn, cameraBlueBtn, discoverBtn, discoverBlueBtn;

        homeBtn = (ImageView) findViewById(R.id.bottom_home);
        homeBlueBtn = (ImageView) findViewById(R.id.bottom_home_blue);
        cameraBtn = (ImageView) findViewById(R.id.bottom_camera);
        cameraBlueBtn = (ImageView) findViewById(R.id.bottom_camera_blue);
        discoverBtn = (ImageView) findViewById(R.id.bottom_discover);
        discoverBlueBtn = (ImageView) findViewById(R.id.bottom_discover_blue);

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HomeActivity.class);
                startActivity(i);
            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CustomCameraActivity.class);
                i.putExtra("task", "share");
                i.putExtra("chain_id", "");
                i.putExtra("creator_id", "");
                startActivity(i);
            }
        });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DiscoverActivity.class);
                startActivity(intent);
            }
        });
    }

}
