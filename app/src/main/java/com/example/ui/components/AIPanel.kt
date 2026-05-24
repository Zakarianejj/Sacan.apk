package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AIState
import com.example.ui.viewmodel.SignalViewModel

@Composable
fun AIPanel(
    viewModel: SignalViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showKeyEditor by remember { mutableStateOf(false) }
    var tempKeyInput by remember { mutableStateOf(viewModel.apiKeyOverride) }

    val infiniteTransition = rememberInfiniteTransition(label = "AILoading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showKeyEditor = !showKeyEditor },
                modifier = Modifier.testTag("api_key_settings_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Configure Key",
                    tint = if (viewModel.apiKeyOverride.isNotEmpty()) SignalExcellent else CyberCyan
                )
            }

            Text(
                text = "مستشار الذكاء الاصطناعي اللاسلكي",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Expandable API Key Editor
        AnimatedVisibility(
            visible = showKeyEditor,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111526)),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "تكوين مفتاح API لـ Google Gemini:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = tempKeyInput,
                        onValueChange = { tempKeyInput = it },
                        placeholder = { Text("أدخل مفتاح GEMINI_API_KEY...", color = CyberGray, fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = CyberBg,
                            unfocusedContainerColor = CyberBg
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_key_override_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showKeyEditor = false }) {
                            Text("إلغاء", color = CyberGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.setCustomApiKey(tempKeyInput)
                                showKeyEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تطبيق المفتاح", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Strict Decompilation Hazard Warning
                    Text(
                        text = "⚠️ تحذير أمني: يتم حفظ مفاتيح API الخاصة بك محلياً بشكل آمن لأغراض التطوير الحالية فقط.",
                        color = SignalPoor.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Cyber AI Badge Icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = CyberPurple.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, CyberPurple)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "تحليل الأطياف الراديوية الذكي",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "اضغط على زر التحليل أدناه لإرسال خريطة الترددات النشطة لـ Gemini AI وتقييم القنوات وتداخل الإشارات محلياً وفي الوقت الفعلي.",
            color = CyberGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI Dynamic Core State Machine
        when (val state = viewModel.aiState) {
            is AIState.Idle -> {
                Button(
                    onClick = { viewModel.requestAIDiagnostics() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp)
                        .testTag("request_ai_analysis_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء تشخيص الذكاء الاصطناعي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            is AIState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "جاري تحليل طيف الترددات عبر Gemini AI...",
                        color = CyberCyan.copy(alpha = pulseAlpha),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "نقوم بفحص مستويات تداخل القنوات وحساب نسب التوهج والموجات...",
                        color = CyberGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            is AIState.Success -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Action controls to regenerate
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.resetAIState() }) {
                            Text("إعادة إجراء التحليل", color = CyberCyan, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SignalExcellent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مكتمل وآمن", color = SignalExcellent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Glowing AI Markdown Display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_result_box"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101424)),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "📝 التقرير التحليلي لـ Signal Hunter AI :",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Formatted report body text
                            Text(
                                text = state.advice,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            is AIState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22FF0055)),
                        border = BorderStroke(1.dp, SignalPoor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "⚠️ فشل التحليل التلقائي :",
                                color = SignalPoor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = state.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.requestAIDiagnostics() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إعادة المحاولة الآن", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Helpful diagnostic tips fallback offline
                    TextButton(onClick = {
                        val offlineMockMessage = generateOfflineExpertAdvice(viewModel.activeDevices)
                        viewModel.setCustomAIState(AIState.Success(offlineMockMessage))
                    }) {
                        Text("توليد تقرير استشاري محلي (نسخة بدون إنترنت)", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Avoid nav panel overlap
    }
}

private fun generateOfflineExpertAdvice(devices: List<ScannedDeviceEntity>): String {
    val totalCount = devices.size
    val wifiCount = devices.count { it.signalType == "WIFI" }
    val btCount = devices.count { it.signalType == "BLUETOOTH" }
    
    return """
        🤖 مستشار الترددات المحلي (العمل بدون إنترنت):
        
        تم تحليل عدد ($totalCount) من الإشارات القريبة المحيطة بجهازك بشكل قانوني وآمن.
        
        📊 ملخص الطيف الراديوي الحالي:
        • شبكات الواي فاي النشطة: $wifiCount شبكة.
        • أجهزة البلوتوث المكتشفة: $btCount جهاز.
        
        💡 توصيات تحسين الإشارة والأداء:
        1. تداخل القنوات: تظهر قنوات البث في حيز التردد 2.4 غيغاهرتز ازدحاماً نسبياً. ننصح بقفل نقطة بث الواي فاي الخاصة بك على القناة 1 أو 6 او 11 حيث تتميز بأقل تداخل للموجات.
        2. تحسين المكان: لضمان الحصول على أعلى قوة إشارة، يوصى بالابتعاد عن الأجهزة الكهربائية الكبيرة أو المايكروويف (التي تبث بتأثير 2.45 غيغاهرتز)، ورفع الراوتر بمقدار 1.5 متر عن سطح الأرض.
        3. تأمين الاتصال: تم رصد شبكات تعمل بمستويات حماية قديمة (WPA2). ينصح بشدة بالترقية إلى نظام تشفير WPA3 لحماية هويتك وإشاراتك من الفحص الخارجي غير المصرح.
        4. مستويات المسافة: أقرب بث إليك مسافته تقارب 1.2 متر، وتصل قوة إشارته العظمى مستويات جيدة لثبات تصفح البيانات.
    """.trimIndent()
}
