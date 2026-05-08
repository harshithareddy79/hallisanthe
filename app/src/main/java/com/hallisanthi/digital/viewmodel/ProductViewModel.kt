package com.hallisanthi.digital.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.hallisanthi.digital.data.db.ProductDatabase
import com.hallisanthi.digital.data.repository.ProductRepository
import com.hallisanthi.digital.models.Product
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var repository: ProductRepository

    private val _searchQuery      = MutableLiveData("")
    private val _selectedCategory = MutableLiveData(Product.CATEGORY_ALL)
    private val _sortOrder        = MutableLiveData(Product.SORT_NEWEST)

    val searchQuery:      LiveData<String> = _searchQuery
    val selectedCategory: LiveData<String> = _selectedCategory
    val sortOrder:        LiveData<String> = _sortOrder

    private val _products = MediatorLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products
    private var currentProductSource: LiveData<List<Product>>? = null

    private val _insertResult = MutableLiveData<Long>()
    val insertResult: LiveData<Long> = _insertResult
    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult
    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult
    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private var _artisanListings: LiveData<List<Product>>? = null
    val artisanListings: LiveData<List<Product>>
        get() = _artisanListings ?: MutableLiveData(emptyList())

    init {
        try {
            val dao = ProductDatabase.getDatabase(application).productDao()
            repository = ProductRepository(dao)
            _products.addSource(_searchQuery)      { requery() }
            _products.addSource(_selectedCategory) { requery() }
            _products.addSource(_sortOrder)        { requery() }
            requery()
        } catch (e: Exception) {
            // If database fails, provide empty list to prevent crash
            _products.postValue(emptyList())
            Log.e("ProductViewModel", "Database initialization failed", e)
        }
    }

    private fun requery() {
        if (!::repository.isInitialized) {
            _products.postValue(emptyList())
            return
        }
        try {
            val query    = _searchQuery.value ?: ""
            val category = _selectedCategory.value ?: Product.CATEGORY_ALL
            currentProductSource?.let { _products.removeSource(it) }
            val src = when {
                query.isBlank() && category == Product.CATEGORY_ALL -> repository.getAllProducts()
                query.isBlank() -> repository.getByCategory(category)
                category == Product.CATEGORY_ALL -> repository.search(query)
                else -> repository.searchInCategory(query, category)
            }
            currentProductSource = src
            _products.addSource(src) { list ->
                _products.value = sortList(list, _sortOrder.value ?: Product.SORT_NEWEST)
            }
        } catch (e: Exception) {
            _products.postValue(emptyList())
            Log.e("ProductViewModel", "Requery failed", e)
        }
    }

    private fun sortList(list: List<Product>, sort: String): List<Product> = when (sort) {
        Product.SORT_PRICE_ASC  -> list.sortedBy { it.price }
        Product.SORT_PRICE_DESC -> list.sortedByDescending { it.price }
        else                    -> list.sortedByDescending { it.createdAt }
    }

    fun setSearchQuery(q: String)  { if (_searchQuery.value != q) _searchQuery.value = q }
    fun setCategory(c: String)     { if (_selectedCategory.value != c) _selectedCategory.value = c }
    fun setSortOrder(s: String)    { if (_sortOrder.value != s) _sortOrder.value = s }

    fun loadProduct(id: Long) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            try {
                _product.postValue(repository.getProductById(id))
            } catch (e: Exception) {
                _product.postValue(null)
                Log.e("ProductViewModel", "Load product failed", e)
            }
        }
    }

    fun loadArtisanListings(phone: String) {
        if (!::repository.isInitialized) return
        try {
            _artisanListings = repository.getBySellerPhone(phone)
        } catch (e: Exception) {
            _artisanListings = MutableLiveData(emptyList())
            Log.e("ProductViewModel", "Load artisan listings failed", e)
        }
    }

    fun insertProduct(product: Product) {
        if (!::repository.isInitialized) {
            _insertResult.postValue(-1L)
            return
        }
        viewModelScope.launch {
            try {
                _insertResult.postValue(repository.insert(product))
            } catch (e: Exception) {
                _insertResult.postValue(-1L)
                Log.e("ProductViewModel", "Insert product failed", e)
            }
        }
    }

    fun updateProduct(product: Product) {
        if (!::repository.isInitialized) {
            _updateResult.postValue(false)
            return
        }
        viewModelScope.launch {
            try { repository.update(product); _updateResult.postValue(true) }
            catch (e: Exception) { _updateResult.postValue(false) }
        }
    }

    fun deleteProduct(product: Product) {
        if (!::repository.isInitialized) {
            _deleteResult.postValue(false)
            return
        }
        viewModelScope.launch {
            try { repository.softDelete(product.id); _deleteResult.postValue(true) }
            catch (e: Exception) { _deleteResult.postValue(false) }
        }
    }

    fun setAvailability(id: Long, available: Boolean) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            try {
                repository.setAvailability(id, available)
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Set availability failed", e)
            }
        }
    }

    fun submitRating(productId: Long, stars: Float) {
        viewModelScope.launch {
            val current = repository.getProductById(productId) ?: return@launch
            val newCount  = current.ratingCount + 1
            val newRating = ((current.rating * current.ratingCount) + stars) / newCount
            repository.updateRating(productId, newRating, newCount)
            _product.postValue(repository.getProductById(productId))
        }
    }
}
