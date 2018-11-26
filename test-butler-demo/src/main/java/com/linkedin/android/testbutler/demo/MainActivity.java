/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.testbutler.demo;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
    }

    private void initUi() {
        Button buttonStartImmersive = (Button) findViewById(R.id.button_switch_to_immersive_mode);
        buttonStartImmersive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
                uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                    uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                }

                getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            }
        });

        Button buttonCallButler = (Button) findViewById(R.id.button_call_butler);
        buttonCallButler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View butlerResponseView = findViewById(R.id.text_butler_response);
                butlerResponseView.setVisibility(View.VISIBLE);
            }
        });
    }

}
