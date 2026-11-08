package com.example.safeher;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class SafetyTipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_safety_tips);

        RecyclerView recyclerView =
                findViewById(R.id.safetyTipsRecyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        List<SafetyTip> tips = new ArrayList<>();

        try {
            XmlPullParser parser =
                    getResources().getXml(R.xml.safety_tips_data);

            tips = XmlParserHelper.parseSafetyTips(parser);

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Unable to load safety tips.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        recyclerView.setAdapter(new SafetyTipAdapter(tips));
    }
}