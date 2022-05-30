/*
 * Copyright 2019 Punch Through Design LLC
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

package com.punchthrough.blestarterappandroid

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import kotlinx.android.synthetic.main.activity_ble_operations.characteristics_recycler_view
import kotlinx.android.synthetic.main.activity_radar.radar
import org.jetbrains.anko.alert
import kotlin.math.cos
import kotlin.math.sin


@RequiresApi(Build.VERSION_CODES.N)
class BleOperationsActivity : AppCompatActivity() {
    private var distances: MutableList<Int> = mutableListOf()

    private lateinit var device: BluetoothDevice
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    // private val characteristics = onlyLastCharacteristic(characteristics0)
    // private val characteristics = lastToFront(characteristics0)
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        super.onCreate(savedInstanceState)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_ble_operations)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Radar"
        }
        setupRecyclerView()
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onlyLastCharacteristic(characteristic: List<BluetoothGattCharacteristic>): List<BluetoothGattCharacteristic> {
        return listOf(characteristic[characteristic.size - 1])
    }

    private fun lastToFront(characteristic: List<BluetoothGattCharacteristic>): List<BluetoothGattCharacteristic> {
        val temp = characteristic.toMutableList()
        val temp2 = characteristic[characteristic.size - 1]
        characteristic.drop(characteristic.size - 1)
        temp.add(0, temp2)
        return temp.toList()
    }

    private fun setupRecyclerView() {
        characteristics_recycler_view.apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@BleOperationsActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = characteristics_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun calculatePosition(angle: Int, distance: Int): Array<Int> {
        val x = distance * sin(Math.PI * 2 * (angle + 90) / 360);
        val y = distance * cos(Math.PI * 2 * (angle + 90) / 360);
        return arrayOf(x.toInt(), y.toInt())
    }

    private fun displayRaindrop(str: String) {
        val ls = str.split("--")
        val angle = ls[0].substring(0, ls[0].length - 1).toInt()
        val distance = ls[1].substring(1, ls[1].length - 2).toInt()

        if (angle % 10 != 0) {
            distances.add(distance)
            radar.addAngle(angle)
        } else
            if (distance < 70) {
                val pos = calculatePosition(angle, distances.average().toInt())
                distances = mutableListOf()
                radar.addRaindrop(pos[0] * 7 + 525, pos[1] * 7 + 525)
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        ConnectionManager.enableNotifications(device, characteristic)
        radar.start()
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }
            onCharacteristicChanged = { _, characteristic ->
                displayRaindrop(String(characteristic.value, charset("UTF-8")))
            }
        }
    }
}
