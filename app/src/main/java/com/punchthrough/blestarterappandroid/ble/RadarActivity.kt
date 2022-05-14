/*
 * Copyright 2022 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid.ble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.R
import kotlinx.android.synthetic.main.activity_radar.*


open class RadarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)

        radar.start()



        btn_start.setOnClickListener {
            radar.addRaindrop(20, 20)
        }

        btn_stop.setOnClickListener {
            radar.stop()
        }
    }
}