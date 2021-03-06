package com.example.myguides

import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myguides.common.*


class GuideActivity : AppCompatActivity() {
    private lateinit var buttonHelper: ImageButtonHelper
    private lateinit var guide: Guide

    private lateinit var guideId: String
    private lateinit var guideNameTextView: TextView
    private lateinit var guideDescriptionTextView: TextView
    private lateinit var bookmarkButton: ImageButton
    private lateinit var slidesListRecyclerView: RecyclerView
    private val client: ApiClient =
        ApiClient(
            "http://10.0.2.2:5000",
            TokenHelper.getToken()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        guideNameTextView = findViewById(R.id.guide_name_textView)
        guideDescriptionTextView = findViewById(R.id.full_guide_description_textView)
        bookmarkButton = findViewById(R.id.bookmark_button)

        slidesListRecyclerView = findViewById(R.id.slides_list_recyclerView)
        slidesListRecyclerView.layoutManager = LinearLayoutManager(this)

        buttonHelper = ImageButtonHelper(bookmarkButton)

        guideId = intent.getStringExtra("current_guide_id") as String

        AsyncRunner.runAsync(
            { client.getFullGuide(guideId) },
            {
                if (!it.isSuccessful()) {
                    val error = it.error
                    AlertWindow.show(this, "Could not get guide. Reason: $error")
                } else {
                    guide = it.value as Guide

                    guideNameTextView.text = guide.description.name
                    guideDescriptionTextView.text = guide.description.description
                    slidesListRecyclerView.adapter = SlideAdapter(guide.slides)

                    if (guide.likes.contains(client.getUserId()))
                        bookmarkButton.setImageResource(android.R.drawable.star_big_on)
                }
            }
        )
    }

    private fun tryLike() {
        AsyncRunner.runAsync(
            { client.likeGuide(guideId) },
            {
                if (!it.isSuccessful()) {
                    val error = it.error
                    AlertWindow.show(
                        this,
                        "Could not like guide $guideId\n Error: $error"
                    )
                } else {
                    guide = client.getFullGuide(guideId).value as Guide
                    bookmarkButton.setImageResource(android.R.drawable.star_big_on)
                }
                buttonHelper.unblock()
            }
        )
    }

    private fun tryDislike() {
        AsyncRunner.runAsync(
            { client.dislikeGuide(guideId) },
            {
                if (!it.isSuccessful()) {
                    val error = it.error
                    AlertWindow.show(
                        this,
                        "Could not dislike guide $guideId\n Error: $error"
                    )
                } else {
                    guide = client.getFullGuide(guideId).value as Guide
                    bookmarkButton.setImageResource(android.R.drawable.star_big_off)
                }
                buttonHelper.unblock()
            }
        )
    }

    fun clickBookmark(view: View) {
        buttonHelper.block()
        AsyncRunner.runAsync(
            { client.getUserId() },
            {
                if (!guide.likes.contains(it))
                    tryLike()
                else
                    tryDislike()
            }
        )
    }

    class SlideAdapter(private val values: List<Slide>) :
        RecyclerView.Adapter<SlideAdapter.ViewHolder>() {
        override fun getItemCount() = values.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.guide_slide_view_list_item, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val decodedString: ByteArray =
                Base64.decode(values[position].base64Image, Base64.DEFAULT)
            val decodedByte =
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            holder.image.setImageBitmap(decodedByte)
            holder.description.text = values[position].text
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var image: ImageView = itemView.findViewById(R.id.guide_slide_view_list_item_image)
            var description: TextView =
                itemView.findViewById(R.id.guide_slide_view_list_item_description)
        }
    }
}
