package com.example.lorarangelogger.ui.tabs

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.data.MeasurementSeries
import com.example.lorarangelogger.databinding.FragmentMeasureBinding
import com.example.lorarangelogger.ui.main.MainViewModel
import java.io.File


private const val TAG = "LoRaMeasureFragment"

class MeasureFragment : Fragment() {
    private var _binding: FragmentMeasureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMeasureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setButtonEnable(viewModel.isOpen.value == true, viewModel.isMeasuring.value == false)
        viewModel.isOpen.observe(viewLifecycleOwner) {
            setButtonEnable(it, viewModel.isMeasuring.value == true)
        }

        updateTextFields(viewModel.measurementSeries)
        viewModel.isMeasuring.observe(viewLifecycleOwner) {
            setButtonEnable(viewModel.isOpen.value == true, it)
            updateTextFields(viewModel.measurementSeries)
        }

        updateLog(viewModel.measureLogData.value!!)
        viewModel.measureLogData.observe(viewLifecycleOwner) { updateLog(it) }
        binding.buttonEcho.setOnClickListener { viewModel.sendEcho() }
        binding.buttonMeasure.setOnClickListener {
            val inputMethodManager =
                requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            val label = binding.editTextLabel.text.toString()
                .replace(";", ",") // can't have ; , since it's the separator in the csv file
            if (doesLabelExist(label)) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Confirm Label")
                builder.setMessage("This label already exists. Are you sure you want to continue?")
                builder.setPositiveButton("Yes") { _, _ -> startMeasurement(label)}
                builder.setNegativeButton("Cancel") { _, _ -> }
                builder.show()
            } else {
                startMeasurement(label)
            }
        }
        binding.buttonCancel.setOnClickListener { viewModel.stopSeries() }
        binding.buttonClearLog.setOnClickListener { viewModel.clearMeasurementLog() }

        binding.textMeasureLabel.setOnLongClickListener { binding.editTextLabel.setText(""); true }
        binding.textMeasureLocation.setOnLongClickListener { binding.editTextLocation.setText(""); true }
        binding.textMeasureDescription.setOnLongClickListener {
            binding.editTextDescription.setText(
                ""
            ); true
        }


    }

    private fun setButtonEnable(isOpen: Boolean, isMeasuring: Boolean) {
        binding.buttonMeasure.isEnabled = isOpen && !isMeasuring
        binding.buttonEcho.isEnabled = isOpen && !isMeasuring
        binding.buttonCancel.isEnabled = isOpen && isMeasuring
    }

    private fun updateTextFields(series: MeasurementSeries) {
        binding.editTextLabel.setText(series.label)
        binding.editTextLocation.setText(series.location)
        binding.editTextDescription.setText(series.description)
        binding.editTextRepetitions.setText(series.repetitions.toString())
        binding.editTextDelay.setText(series.delay.toString())
    }

    private fun startMeasurement(label: String) {
        val location = binding.editTextLocation.text.toString().replace(";", ",")
        val description = binding.editTextDescription.text.toString().replace(";", ",")
        val repetitionsStr = binding.editTextRepetitions.text.toString()
        val delayStr = binding.editTextDelay.text.toString()
        if (label == "" || location == "" || description == "" || repetitionsStr == "" || delayStr == "") {
            Toast.makeText(requireContext(), "Text must not be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        val repetitions = repetitionsStr.toInt()
        val delay = delayStr.toInt()
        if (repetitions < 1 || repetitions > 100) {
            Toast.makeText(
                requireContext(),
                "Invalid # of Packets! (1-100 is allowed)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (delay < 1 || delay > 300) {
            Toast.makeText(requireContext(), "Invalid delay (1-300 is allowed)", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val series =
            MeasurementSeries(label, location, description, repetitions, delay)
        viewModel.startSeries(series)
    }

    private fun updateLog(list: List<String>) {
        binding.buttonClearLog.isEnabled = list.isNotEmpty()
        binding.textMeasureLog.text = list.joinToString("\n\n")
    }

    private fun doesLabelExist(label: String) : Boolean {
        try {
            if (File(requireActivity().filesDir, viewModel.fileName).exists()) {
                val reader = requireActivity().openFileInput(viewModel.fileName).bufferedReader()
                val lines = reader.use {
                    it.readLines()
                }
                for (line in lines ){
                    val data = line.split(";")
                    if (data.size == 12 && data[0] == label) return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "An exception while reading the file occurred.")
        }
        return false
    }
}