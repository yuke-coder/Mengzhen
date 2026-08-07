package com.mengzhen.app.ui.screens

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Base64
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.NavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.target
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.ui.feedback.AppNotice
import com.mengzhen.app.ui.navigation.Screen
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private data class FeedbackChoice(
    val value: String,
    val title: String,
    val tips: String = "",
)

private val feedbackTypes = listOf(
    FeedbackChoice("bug", "Bug 缺陷", "功能异常"),
    FeedbackChoice("suggestion", "产品建议", "体验与功能"),
)

private val feedbackScenes = mapOf(
    "bug" to listOf(
        FeedbackChoice("播放与声音", "播放与声音"),
        FeedbackChoice("定时任务", "定时任务"),
        FeedbackChoice("音频导入", "音频导入"),
        FeedbackChoice("账号与资料", "账号与资料"),
        FeedbackChoice("其他问题", "其他问题"),
    ),
    "suggestion" to listOf(
        FeedbackChoice("功能建议", "功能建议"),
        FeedbackChoice("体验优化", "体验优化"),
        FeedbackChoice("其他建议", "其他建议"),
    ),
)

private data class FeedbackSummary(
    val id: String,
    val type: String,
    val category: String?,
    val content: String,
    val status: Int,
    val opGroup: String?,
    val opName: String?,
    val processedAt: String?,
    val createdAt: String,
)

private data class FeedbackMessage(
    val id: String,
    val sender: String,
    val content: String,
    val images: List<String>,
    val createdAt: String,
)

private data class FeedbackRecord(
    val summary: FeedbackSummary,
    val contact: String?,
    val images: List<String>,
    val replies: List<FeedbackMessage>,
) {
    val messages: List<FeedbackMessage>
        get() = listOf(
            FeedbackMessage(
                id = summary.id,
                sender = "我",
                content = summary.content,
                images = images,
                createdAt = summary.createdAt,
            ),
        ) + replies
}

@Composable
fun XimalayaFeedbackChooseTypeScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember(context) { TaskStore.get(context) }

    LaunchedEffect(Unit) {
        if (store.getSession() == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackChooseType.route) { inclusive = true }
            }
        }
    }

    AndroidView(
        factory = { FeedbackChooseTypeBinding.inflate(it).root },
        update = { root ->
            (root.tag as FeedbackChooseTypeBinding).bind(
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate(Screen.FeedbackHistory.route) },
                onTypeSelected = {
                    navController.navigate(Screen.FeedbackDetail.createRoute(it))
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaFeedbackDetailScreen(
    navController: NavController,
    feedbackType: String,
) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    val store = remember(context) { TaskStore.get(context) }
    val scope = rememberCoroutineScope()
    val normalizedType = feedbackType.takeIf(feedbackScenes::containsKey) ?: "suggestion"
    val scenes = feedbackScenes.getValue(normalizedType)
    var selectedScene by remember(normalizedType) { mutableStateOf<String?>(null) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (store.getSession() == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackDetail.route) { inclusive = true }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { selected ->
        val remaining = (6 - imageUris.size).coerceAtLeast(0)
        imageUris = imageUris + selected.filterNot(imageUris::contains).take(remaining)
        if (selected.size > remaining) AppNotice.info(context, "最多可上传6张图片")
    }

    fun submit(scene: String, content: String, contact: String) {
        if (submitting) return
        val detail = content.trim()
        val contactValue = contact.trim()
        if (detail.isEmpty()) {
            AppNotice.warning(context, "请填写反馈内容")
            return
        }
        if (
            contactValue.isNotEmpty() &&
            !Patterns.EMAIL_ADDRESS.matcher(contactValue).matches() &&
            !Regex("^1\\d{10}$").matches(contactValue)
        ) {
            AppNotice.warning(context, "请填写正确的手机号或者邮箱")
            return
        }

        submitting = true
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching {
                    api.submitFeedback(
                        type = normalizedType,
                        category = scene,
                        content = detail,
                        contact = contactValue.ifBlank { null },
                        images = imageUris.map { encodeFeedbackImage(context, it) },
                    )
                }.getOrElse {
                    JSONObject().put("success", false).put("message", it.message ?: "提交失败")
                }
            }
            submitting = false
            when {
                response.optBoolean("success", false) -> {
                    navController.navigate(Screen.FeedbackSuccess.route) {
                        popUpTo(Screen.FeedbackDetail.route) { inclusive = true }
                    }
                }
                response.optBoolean("sessionExpired", false) -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.FeedbackDetail.route) { inclusive = true }
                    }
                }
                else -> AppNotice.error(
                    context,
                    response.optString("message").ifBlank {
                        response.optString("error", "提交失败")
                    },
                )
            }
        }
    }

    AndroidView(
        factory = { FeedbackDetailBinding.inflate(it).root },
        update = { root ->
            (root.tag as FeedbackDetailBinding).bind(
                choices = scenes,
                selectedChoice = selectedScene,
                images = imageUris,
                submitting = submitting,
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate(Screen.FeedbackHistory.route) },
                onChoiceSelected = { selectedScene = it },
                onAddImage = { imagePicker.launch("image/*") },
                onPreviewImage = { showFeedbackImage(context, it) },
                onRemoveImage = { uri -> imageUris = imageUris - uri },
                onSubmit = ::submit,
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaFeedbackSuccessScreen(navController: NavController) {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feed_back_success_source, null, false)
        },
        update = { root ->
            root.bindFeedbackTitle(
                title = "反馈问题成功",
                rightText = "反馈记录",
                onBack = { navController.popBackStack() },
                onRight = { navController.navigate(Screen.FeedbackHistory.route) },
            )
            root.findViewById<View>(R.id.main_finish).setOnClickListener {
                if (!navController.popBackStack()) {
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaFeedbackHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    val store = remember(context) { TaskStore.get(context) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<FeedbackSummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (store.getSession() == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackHistory.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        val response = withContext(Dispatchers.IO) {
            runCatching { api.getFeedbacks() }.getOrNull()
        }
        loading = false
        if (response?.optBoolean("sessionExpired", false) == true) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackHistory.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        if (response?.optBoolean("success", false) != true) {
            error = response?.optString("message")?.ifBlank { null } ?: "反馈记录加载失败"
            return@LaunchedEffect
        }
        records = response.optJSONArray("feedbacks").toFeedbackSummaries()
    }

    AndroidView(
        factory = { FeedbackHistoryBinding.inflate(it).root },
        update = { root ->
            (root.tag as FeedbackHistoryBinding).bind(
                loading = loading,
                error = error,
                records = records,
                onBack = { navController.popBackStack() },
                onRecord = { id -> navController.navigate(Screen.FeedbackRecord.createRoute(id)) },
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaFeedbackRecordScreen(navController: NavController, feedbackId: String) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var replying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var record by remember(feedbackId) { mutableStateOf<FeedbackRecord?>(null) }

    LaunchedEffect(feedbackId, reloadKey) {
        loading = record == null
        error = null
        val response = withContext(Dispatchers.IO) {
            runCatching { api.getFeedback(feedbackId) }.getOrNull()
        }
        loading = false
        if (response?.optBoolean("sessionExpired", false) == true) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackRecord.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        record = response?.takeIf { it.optBoolean("success", false) }
            ?.toFeedbackRecord()
        if (record == null) {
            error = response?.optString("message", "反馈记录加载失败") ?: "反馈记录加载失败"
        }
    }

    fun reply(content: String, onSent: () -> Unit) {
        if (replying) return
        val value = content.trim()
        if (value.isEmpty()) {
            AppNotice.warning(context, "回复内容不得为空")
            return
        }
        replying = true
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching { api.replyFeedback(feedbackId, value) }.getOrNull()
            }
            replying = false
            when {
                response?.optBoolean("success", false) == true -> {
                    onSent()
                    reloadKey++
                }
                response?.optInt("status") == 999 -> {
                    AppNotice.warning(context, "该反馈已关闭，请重新提交")
                    reloadKey++
                }
                response?.optBoolean("sessionExpired", false) == true -> {
                    navController.navigate(Screen.Login.route)
                }
                else -> AppNotice.error(
                    context,
                    response?.optString("message", "回复失败") ?: "回复失败",
                )
            }
        }
    }

    AndroidView(
        factory = { FeedbackRecordBinding.inflate(it).root },
        update = { root ->
            (root.tag as FeedbackRecordBinding).bind(
                loading = loading,
                replying = replying,
                error = error,
                record = record,
                onBack = { navController.popBackStack() },
                onInfo = {
                    navController.navigate(Screen.FeedbackRecordInfo.createRoute(feedbackId))
                },
                onReply = ::reply,
                onPreview = { showFeedbackImage(context, it) },
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun XimalayaFeedbackRecordInfoScreen(navController: NavController, feedbackId: String) {
    val context = LocalContext.current
    val api = remember(context) { ApiClient.get(context) }
    var record by remember(feedbackId) { mutableStateOf<FeedbackRecord?>(null) }

    LaunchedEffect(feedbackId) {
        val response = withContext(Dispatchers.IO) {
            runCatching { api.getFeedback(feedbackId) }.getOrNull()
        }
        if (response?.optBoolean("sessionExpired", false) == true) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.FeedbackRecordInfo.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        record = response?.takeIf { it.optBoolean("success", false) }
            ?.toFeedbackRecord()
        if (record == null) {
            AppNotice.error(
                context,
                response?.optString("message", "反馈详情加载失败") ?: "反馈详情加载失败",
            )
            navController.popBackStack()
        }
    }

    AndroidView(
        factory = { FeedbackInfoBinding.inflate(it).root },
        update = { root ->
            (root.tag as FeedbackInfoBinding).bind(
                record = record,
                onBack = { navController.popBackStack() },
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private class FeedbackChooseTypeBinding private constructor(
    val root: View,
    private val container: LinearLayout,
) {
    private var bound = false

    fun bind(
        onBack: () -> Unit,
        onHistory: () -> Unit,
        onTypeSelected: (String) -> Unit,
    ) {
        root.bindFeedbackTitle("意见反馈", "反馈记录", onBack, onHistory)
        if (bound) return
        bound = true
        feedbackTypes.forEach { choice ->
            val row = LayoutInflater.from(root.context)
                .inflate(R.layout.main_item_feed_back_question_new_source, container, false)
            row.findViewById<TextView>(R.id.main_item_title).text = choice.title
            row.findViewById<TextView>(R.id.main_tips).text = choice.tips
            row.setOnClickListener { onTypeSelected(choice.value) }
            container.addView(row)
        }
    }

    companion object {
        fun inflate(context: Context): FeedbackChooseTypeBinding {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feedback_choose_type_source, null, false)
            return FeedbackChooseTypeBinding(
                root,
                root.findViewById(R.id.main_feedback_type_container),
            ).also { root.tag = it }
        }
    }
}

private class FeedbackDetailBinding private constructor(
    val root: View,
    private val typeList: ListView,
    private val content: EditText,
    private val contact: EditText,
    private val counter: TextView,
    private val imageGrid: GridView,
    private val addImage: View,
    private val submit: TextView,
) {
    private val typeAdapter = FeedbackChoiceAdapter(root.context)
    private val imageAdapter = FeedbackImageAdapter(root.context, editable = true)
    private var selectedChoice: String? = null
    private var submitting = false
    private var onSubmit: (String, String, String) -> Unit = { _, _, _ -> }

    init {
        typeList.adapter = typeAdapter
        imageGrid.adapter = imageAdapter
        content.doAfterTextChanged {
            counter.text = "${it?.length ?: 0} / 200"
            updateSubmitState()
        }
        submit.setOnClickListener {
            val choice = selectedChoice ?: return@setOnClickListener
            onSubmit(choice, content.text.toString(), contact.text.toString())
        }
    }

    fun bind(
        choices: List<FeedbackChoice>,
        selectedChoice: String?,
        images: List<Uri>,
        submitting: Boolean,
        onBack: () -> Unit,
        onHistory: () -> Unit,
        onChoiceSelected: (String) -> Unit,
        onAddImage: () -> Unit,
        onPreviewImage: (Uri) -> Unit,
        onRemoveImage: (Uri) -> Unit,
        onSubmit: (String, String, String) -> Unit,
    ) {
        root.bindFeedbackTitle("意见反馈", "反馈记录", onBack, onHistory)
        this.selectedChoice = selectedChoice
        this.submitting = submitting
        this.onSubmit = onSubmit
        typeAdapter.bind(choices, selectedChoice)
        typeList.setOnItemClickListener { _, _, position, _ ->
            onChoiceSelected(choices[position].value)
        }
        imageAdapter.bind(
            models = images,
            onPreview = { model -> onPreviewImage(model as Uri) },
            onRemove = { model -> onRemoveImage(model as Uri) },
        )
        imageGrid.fitFeedbackRows(images.size)
        addImage.visibility = if (images.size < 6) View.VISIBLE else View.GONE
        addImage.setOnClickListener { onAddImage() }
        submit.text = if (submitting) "发送中" else "发送"
        updateSubmitState()
    }

    private fun updateSubmitState() {
        submit.isEnabled = !submitting && selectedChoice != null &&
            content.text?.toString()?.trim()?.isNotEmpty() == true
    }

    companion object {
        fun inflate(context: Context): FeedbackDetailBinding {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feed_back_detail_source, null, false)
            return FeedbackDetailBinding(
                root = root,
                typeList = root.findViewById(R.id.main_feedback_type_list),
                content = root.findViewById(R.id.main_et_content),
                contact = root.findViewById(R.id.main_et_num),
                counter = root.findViewById(R.id.main_tv_words),
                imageGrid = root.findViewById(R.id.main_img_gridview),
                addImage = root.findViewById(R.id.main_iv_add_img),
                submit = root.findViewById(R.id.main_submit),
            ).also { root.tag = it }
        }
    }
}

private class FeedbackChoiceAdapter(private val context: Context) : BaseAdapter() {
    private var choices: List<FeedbackChoice> = emptyList()
    private var selectedChoice: String? = null

    fun bind(choices: List<FeedbackChoice>, value: String?) {
        if (this.choices == choices && selectedChoice == value) return
        this.choices = choices
        selectedChoice = value
        notifyDataSetChanged()
    }

    override fun getCount(): Int = choices.size
    override fun getItem(position: Int): FeedbackChoice = choices[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val root = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.main_item_feedback_question_category_source, parent, false)
        val item = getItem(position)
        root.findViewById<TextView>(R.id.main_item_title).text = item.title
        root.findViewById<CheckBox>(R.id.main_check_box).isChecked =
            item.value == selectedChoice
        return root
    }
}

private class FeedbackImageAdapter(
    private val context: Context,
    private val editable: Boolean,
) : BaseAdapter() {
    private var models: List<Any> = emptyList()
    private var onPreview: (Any) -> Unit = {}
    private var onRemove: (Any) -> Unit = {}

    fun bind(models: List<*>, onPreview: (Any) -> Unit, onRemove: (Any) -> Unit = {}) {
        this.models = models.filterNotNull()
        this.onPreview = onPreview
        this.onRemove = onRemove
        notifyDataSetChanged()
    }

    override fun getCount(): Int = models.size
    override fun getItem(position: Int): Any = models[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = if (convertView == null) {
            FeedbackImageHolder(context, editable).also { it.root.tag = it }
        } else {
            convertView.tag as FeedbackImageHolder
        }
        holder.bind(getItem(position), onPreview, onRemove)
        return holder.root
    }
}

private class FeedbackImageHolder(context: Context, editable: Boolean) {
    val root = FrameLayout(context).apply {
        layoutParams = AbsListView.LayoutParams(context.dp(80), context.dp(80))
    }
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        root.addView(
            this,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
    private val remove = TextView(context).apply {
        text = "×"
        textSize = 17f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = ColorDrawable(0x99000000.toInt())
        visibility = if (editable) View.VISIBLE else View.GONE
        root.addView(
            this,
            FrameLayout.LayoutParams(context.dp(24), context.dp(24), Gravity.END or Gravity.TOP),
        )
    }

    fun bind(model: Any, onPreview: (Any) -> Unit, onRemove: (Any) -> Unit) {
        image.setImageDrawable(null)
        if (model is String && model.startsWith("data:image")) {
            decodeDataImage(model)?.let(image::setImageBitmap)
        } else {
            image.context.imageLoader.enqueue(
                ImageRequest.Builder(image.context)
                    .data(model)
                    .target(image)
                    .build(),
            )
        }
        root.setOnClickListener { onPreview(model) }
        remove.setOnClickListener { onRemove(model) }
    }
}

private class FeedbackHistoryBinding private constructor(
    val root: View,
    private val container: LinearLayout,
    private val state: TextView,
) {
    private var recordKeys: List<String> = emptyList()

    fun bind(
        loading: Boolean,
        error: String?,
        records: List<FeedbackSummary>,
        onBack: () -> Unit,
        onRecord: (String) -> Unit,
    ) {
        root.bindFeedbackTitle("反馈记录", onBack = onBack)
        state.text = when {
            loading -> "加载中"
            error != null -> error
            records.isEmpty() -> "暂无反馈记录"
            else -> ""
        }
        state.visibility = if (loading || error != null || records.isEmpty()) View.VISIBLE else View.GONE
        val keys = records.map { "${it.id}:${it.status}:${it.updatedKey()}" }
        if (keys == recordKeys) return
        recordKeys = keys
        container.removeAllViews()
        records.forEach { record ->
            val item = LayoutInflater.from(root.context)
                .inflate(R.layout.main_item_feedback_history_source, container, false)
            item.findViewById<TextView>(R.id.main_title).text =
                record.category?.takeIf(String::isNotBlank)
                    ?: record.content.lineSequence().firstOrNull().orEmpty()
                    .ifBlank { "[图片]" }
            item.findViewById<TextView>(R.id.main_time).text =
                formatFeedbackTime(record.createdAt)
            item.findViewById<TextView>(R.id.main_status).text =
                feedbackStatusLabel(record.status)
            item.setOnClickListener { onRecord(record.id) }
            container.addView(item)
        }
    }

    companion object {
        fun inflate(context: Context): FeedbackHistoryBinding {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feedback_history_source, null, false)
            return FeedbackHistoryBinding(
                root,
                root.findViewById(R.id.main_feedback_history_container),
                root.findViewById(R.id.main_feedback_history_state),
            ).also { root.tag = it }
        }
    }
}

private class FeedbackRecordBinding private constructor(
    val root: View,
    private val scroll: ScrollView,
    private val messages: LinearLayout,
    private val state: TextView,
    private val input: View,
    private val reply: EditText,
    private val send: TextView,
) {
    private var messageKeys: List<String> = emptyList()

    fun bind(
        loading: Boolean,
        replying: Boolean,
        error: String?,
        record: FeedbackRecord?,
        onBack: () -> Unit,
        onInfo: () -> Unit,
        onReply: (String, () -> Unit) -> Unit,
        onPreview: (String) -> Unit,
    ) {
        root.bindFeedbackTitle(
            title = "反馈详情",
            rightText = if (record == null) null else "详细信息",
            onBack = onBack,
            onRight = onInfo,
        )
        state.text = when {
            loading -> "加载中"
            error != null -> error
            else -> ""
        }
        state.visibility = if (loading || error != null) View.VISIBLE else View.GONE
        input.visibility = if (record != null && record.summary.status != 3) {
            View.VISIBLE
        } else {
            View.GONE
        }
        send.text = if (replying) "发送中" else "发送"
        send.isEnabled = !replying
        send.setOnClickListener {
            onReply(reply.text.toString()) { reply.setText("") }
        }

        val rows = record?.messages.orEmpty()
        val keys = rows.map { "${it.id}:${it.content}:${it.images.size}" }
        if (keys == messageKeys) return
        messageKeys = keys
        messages.removeAllViews()
        rows.forEach { message ->
            val row = LayoutInflater.from(root.context)
                .inflate(R.layout.main_item_feed_back_order_detail_source, messages, false)
            row.findViewById<TextView>(R.id.main_title).text =
                message.content.ifBlank { "[图片]" }
            row.findViewById<TextView>(R.id.main_time).text =
                formatFeedbackTime(message.createdAt)
            row.findViewById<TextView>(R.id.main_status).text = message.sender
            row.findViewById<GridView>(R.id.main_dynamic_grid).apply {
                val adapter = FeedbackImageAdapter(root.context, editable = false)
                this.adapter = adapter
                adapter.bind(message.images, onPreview = { onPreview(it as String) })
                fitFeedbackRows(message.images.size)
            }
            messages.addView(row)
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    companion object {
        fun inflate(context: Context): FeedbackRecordBinding {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feedback_order_detail_source, null, false)
            return FeedbackRecordBinding(
                root = root,
                scroll = root.findViewById(R.id.main_feedback_messages_scroll),
                messages = root.findViewById(R.id.main_feedback_messages),
                state = root.findViewById(R.id.main_feedback_record_state),
                input = root.findViewById(R.id.main_input_lauout),
                reply = root.findViewById(R.id.main_reply),
                send = root.findViewById(R.id.main_send),
            ).also { root.tag = it }
        }
    }
}

private class FeedbackInfoBinding private constructor(
    val root: View,
) {
    fun bind(record: FeedbackRecord?, onBack: () -> Unit) {
        root.bindFeedbackTitle("详细信息", onBack = onBack)
        record ?: return
        val summary = record.summary
        root.findViewById<View>(R.id.main_opgroup).visibility =
            if (summary.status == 1) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.main_opname).visibility =
            if (summary.status == 1) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.main_processtime).visibility =
            if (summary.status == 1) View.GONE else View.VISIBLE
        root.findViewById<TextView>(R.id.main_tv_opgroup).text =
            summary.opGroup.orEmpty()
        root.findViewById<TextView>(R.id.main_tv_opname).text =
            summary.opName.orEmpty()
        root.findViewById<TextView>(R.id.main_tv_created).text =
            formatFeedbackTime(summary.createdAt)
        root.findViewById<TextView>(R.id.main_tv_processtime).text =
            summary.processedAt?.let(::formatFeedbackTime).orEmpty()
        root.findViewById<TextView>(R.id.main_tv_status).text =
            feedbackStatusLabel(summary.status)
    }

    companion object {
        fun inflate(context: Context): FeedbackInfoBinding {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.main_fra_feed_back_order_detail_info_source, null, false)
            return FeedbackInfoBinding(root).also { root.tag = it }
        }
    }
}

private fun View.bindFeedbackTitle(
    title: String,
    rightText: String? = null,
    onBack: () -> Unit,
    onRight: () -> Unit = {},
) {
    findViewById<TextView>(R.id.ximalaya_title_text).text = title
    findViewById<View>(R.id.ximalaya_title_back).apply {
        visibility = View.VISIBLE
        setOnClickListener { onBack() }
    }
    findViewById<TextView>(R.id.ximalaya_title_right).apply {
        visibility = if (rightText == null) View.GONE else View.VISIBLE
        text = rightText.orEmpty()
        setOnClickListener { onRight() }
    }
}

private fun GridView.fitFeedbackRows(itemCount: Int) {
    visibility = if (itemCount == 0) View.GONE else View.VISIBLE
    val rows = (itemCount + 2) / 3
    layoutParams = layoutParams.apply {
        height = if (rows == 0) 0 else context.dp(rows * 80 + (rows - 1) * 10)
    }
}

private fun JSONArray?.toFeedbackSummaries(): List<FeedbackSummary> =
    (0 until (this?.length() ?: 0)).mapNotNull { index ->
        this?.optJSONObject(index)?.toFeedbackSummary()
    }

private fun JSONObject.toFeedbackSummary(): FeedbackSummary? {
    val id = optString("id")
    if (id.isBlank()) return null
    return FeedbackSummary(
        id = id,
        type = optString("type"),
        category = nullableString("category"),
        content = optString("content"),
        status = optInt("status", 1),
        opGroup = nullableString("op_group"),
        opName = nullableString("op_name"),
        processedAt = nullableString("processed_at"),
        createdAt = optString("created_at"),
    )
}

private fun JSONObject.toFeedbackRecord(): FeedbackRecord? {
    val feedback = optJSONObject("feedback") ?: return null
    val summary = feedback.toFeedbackSummary() ?: return null
    val replies = optJSONArray("replies")
    return FeedbackRecord(
        summary = summary,
        contact = feedback.nullableString("contact"),
        images = feedback.optJSONArray("images").toStringList(),
        replies = (0 until (replies?.length() ?: 0)).mapNotNull { index ->
            replies?.optJSONObject(index)?.toFeedbackMessage()
        },
    )
}

private fun JSONObject.toFeedbackMessage(): FeedbackMessage? {
    val id = optString("id")
    if (id.isBlank()) return null
    return FeedbackMessage(
        id = id,
        sender = nullableString("sender") ?: "客服",
        content = optString("content"),
        images = optJSONArray("images").toStringList(),
        createdAt = optString("created_at"),
    )
}

private fun JSONObject.nullableString(name: String): String? =
    optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONArray?.toStringList(): List<String> =
    (0 until (this?.length() ?: 0)).mapNotNull { this?.optString(it) }

private fun FeedbackSummary.updatedKey(): String =
    listOf(category, content, opGroup, opName, processedAt, createdAt).joinToString("|")

private fun feedbackStatusLabel(status: Int): String = when (status) {
    2 -> "受理中"
    3 -> "受理完毕"
    else -> "尚未受理"
}

private fun formatFeedbackTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA),
    )
}.getOrDefault(value)

private fun encodeFeedbackImage(context: Context, uri: Uri): String {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val maxDimension = maxOf(info.size.width, info.size.height)
        if (maxDimension > 1280) {
            val scale = 1280f / maxDimension
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    return try {
        ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output))
            "data:image/jpeg;base64," +
                Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    } finally {
        bitmap.recycle()
    }
}

private fun showFeedbackImage(context: Context, model: Any) {
    val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    val image = ImageView(context).apply {
        setBackgroundColor(Color.BLACK)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setOnClickListener { dialog.dismiss() }
    }
    if (model is String && model.startsWith("data:image")) {
        decodeDataImage(model)?.let(image::setImageBitmap)
    } else {
        context.imageLoader.enqueue(
            ImageRequest.Builder(context).data(model).target(image).build(),
        )
    }
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(image)
    dialog.show()
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
    )
}

private fun decodeDataImage(value: String): Bitmap? = runCatching {
    val bytes = Base64.decode(value.substringAfter(','), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

private fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
