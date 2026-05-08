package com.hallisanthi.digital.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hallisanthi.digital.R

/**
 * Feature 18: Onboarding Screens – shown once on first launch.
 */
class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "onboarding"
        private const val KEY_DONE = "done"

        fun shouldShow(ctx: Context): Boolean =
            !ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

        fun markDone(ctx: Context) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_DONE, true).apply()
        }
    }

    data class OnboardPage(val emoji: String, val title: String, val desc: String)

    private val pages = listOf(
        OnboardPage("🏪", "Welcome to Halli-Santhe Digital", "Your village market, now in your pocket. Discover authentic Indian handicrafts from local artisans."),
        OnboardPage("🛒", "Browse & Discover", "Explore pottery, textiles, bamboo crafts, jewelry and more. Filter by category or search for what you love."),
        OnboardPage("🧑‍🎨", "Are You an Artisan?", "List your products easily with photos and pricing. Reach buyers across Karnataka and beyond!"),
        OnboardPage("💬", "Connect Directly on WhatsApp", "No middleman. Contact sellers directly on WhatsApp and negotiate the best deal.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.onboardingViewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val nextBtn   = findViewById<MaterialButton>(R.id.nextButton)
        val skipBtn   = findViewById<TextView>(R.id.skipButton)

        viewPager.adapter = OnboardingAdapter(pages)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == pages.lastIndex) {
                    nextBtn.text = "Get Started 🚀"
                } else {
                    nextBtn.text = "Next →"
                }
            }
        })

        nextBtn.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.lastIndex) {
                viewPager.currentItem = current + 1
            } else {
                finish()
            }
        }

        skipBtn.setOnClickListener { finish() }
    }

    override fun finish() {
        markDone(this)
        startActivity(Intent(this, LoginActivity::class.java))
        super.finish()
    }
}

class OnboardingAdapter(private val pages: List<OnboardingActivity.OnboardPage>) :
    RecyclerView.Adapter<OnboardingAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboard_page, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(pages[position])
    override fun getItemCount() = pages.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(page: OnboardingActivity.OnboardPage) {
            itemView.findViewById<TextView>(R.id.onboardEmoji).text  = page.emoji
            itemView.findViewById<TextView>(R.id.onboardTitle).text  = page.title
            itemView.findViewById<TextView>(R.id.onboardDesc).text   = page.desc
        }
    }
}
