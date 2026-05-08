package com.hallisanthi.digital.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.UserSession
import com.hallisanthi.digital.databinding.ActivityArtisanUploadBinding
import com.hallisanthi.digital.models.Product
import com.hallisanthi.digital.viewmodel.ProductViewModel
import java.io.File
import java.io.FileOutputStream

class ArtisanUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtisanUploadBinding
    private val viewModel: ProductViewModel by viewModels()
    private var selectedImagePath: String = ""
    private var insertObserved = false
    private var editingProduct: Product? = null

    companion object {
        const val EXTRA_EDIT_PRODUCT_ID = "edit_product_id"
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageUri(it) }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickImage.launch("image/*")
        else Toast.makeText(this, getString(R.string.permission_storage_denied), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Role guard: only Artisans can upload
        if (!UserSession.isArtisan(this)) {
            Toast.makeText(this, "Only sellers can upload products", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityArtisanUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupCategoryDropdown()
        setupClickListeners()

        // ── Pre-fill from session (read-only) ──────────────────────────────
        val sessionPhone = UserSession.getPhone(this)
        binding.sellerPhoneEditText.setText(sessionPhone)
        binding.sellerPhoneEditText.isEnabled = false     // Seller phone is auto-filled
        binding.sellerPhoneLayout.helperText  = "Linked to your account"

        val sessionName = UserSession.getName(this)
        val artisanField = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText?>(R.id.artisanNameEditText)
        artisanField?.setText(sessionName)
        artisanField?.isEnabled = false

        // ── Edit mode ──────────────────────────────────────────────────────
        val editId = intent.getLongExtra(EXTRA_EDIT_PRODUCT_ID, -1L)
        if (editId != -1L) {
            supportActionBar?.title = "Edit Product"
            binding.submitButton.text = "Update Product"
            viewModel.loadProduct(editId)
            viewModel.product.observe(this) { product ->
                if (product != null && editingProduct == null) {
                    editingProduct = product
                    prefillForm(product)
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.upload_product_title)
            observeInsertResult()
        }
    }

    private fun prefillForm(product: Product) {
        binding.productNameEditText.setText(product.name)
        binding.productPriceEditText.setText(product.price.toLong().toString())
        binding.productCategoryAutoComplete.setText(product.category, false)
        // sellerPhone is already pre-filled from session — don't override with old value
        selectedImagePath = product.imagePath

        val descField = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText?>(R.id.descriptionEditText)
        descField?.setText(product.description)

        if (product.imagePath.isNotBlank()) {
            val file = File(product.imagePath)
            if (file.exists()) {
                com.bumptech.glide.Glide.with(this).load(file).into(binding.productImageView)
                binding.productImageView.visibility = View.VISIBLE
                binding.imagePlaceholder.visibility = View.GONE
            }
        }

        viewModel.updateResult.observe(this) { success ->
            showLoading(false)
            if (success) { Toast.makeText(this, "Product updated ✅", Toast.LENGTH_SHORT).show(); finish() }
            else Toast.makeText(this, "Update failed. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Product.getAllCategories())
        binding.productCategoryAutoComplete.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.imagePreviewCard.setOnClickListener { requestImagePick() }

        binding.submitButton.setOnClickListener {
            val name     = binding.productNameEditText.text.toString().trim()
            val priceStr = binding.productPriceEditText.text.toString().trim()
            val category = binding.productCategoryAutoComplete.text.toString().trim()
            // Phone always comes from session — not user input
            val phone    = UserSession.getPhone(this)
            val desc     = binding.root.findViewById<com.google.android.material.textfield.TextInputEditText?>(R.id.descriptionEditText)
                ?.text.toString().trim()
            val artisan  = UserSession.getName(this)

            if (!validateInputs(name, priceStr, category)) return@setOnClickListener
            val price = priceStr.toDoubleOrNull() ?: 0.0
            showLoading(true)

            val existing = editingProduct
            if (existing != null) {
                viewModel.updateProduct(existing.copy(
                    name = name, price = price, category = category,
                    sellerPhone = phone,           // always from session
                    artisanName = artisan,
                    description = desc,
                    imagePath = if (selectedImagePath.isNotBlank()) selectedImagePath else existing.imagePath
                ))
            } else {
                viewModel.insertProduct(Product(
                    name = name, price = price, category = category,
                    sellerPhone = phone,           // always from session
                    artisanName = artisan,
                    description = desc,
                    imagePath = selectedImagePath
                ))
            }
        }
    }

    /** Validate without touching phone (it's locked to session). */
    private fun validateInputs(name: String, price: String, category: String): Boolean {
        var valid = true

        if (name.length < 2) {
            binding.productNameLayout.error = "Name must be at least 2 characters"
            valid = false
        } else {
            binding.productNameLayout.error = null
        }

        val p = price.toDoubleOrNull()
        if (p == null || p <= 0) {
            binding.productPriceLayout.error = "Enter a valid price greater than 0"
            valid = false
        } else {
            binding.productPriceLayout.error = null
        }

        if (category.isBlank()) {
            binding.productCategoryLayout.error = "Please select a category"
            valid = false
        } else {
            binding.productCategoryLayout.error = null
        }

        return valid
    }

    private fun requestImagePick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            pickImage.launch("image/*")
        else requestPermission.launch(permission)
    }

    @Suppress("DEPRECATION")
    private fun handleImageUri(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val compressed = compressBitmap(bitmap)
            selectedImagePath = saveBitmapToFile(compressed)
            binding.productImageView.setImageBitmap(compressed)
            binding.productImageView.visibility = View.VISIBLE
            binding.imagePlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compressBitmap(b: Bitmap): Bitmap {
        val maxSize = 800
        val ratio = minOf(maxSize.toFloat() / b.width, maxSize.toFloat() / b.height)
        if (ratio >= 1f) return b
        return Bitmap.createScaledBitmap(b, (b.width * ratio).toInt(), (b.height * ratio).toInt(), true)
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String {
        val dir  = File(filesDir, "product_images").also { it.mkdirs() }
        val file = File(dir, "product_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file.absolutePath
    }

    private fun observeInsertResult() {
        insertObserved = true
        viewModel.insertResult.observe(this) { id ->
            if (!insertObserved) return@observe
            showLoading(false)
            if (id > 0) {
                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.upload_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.submitButton.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
