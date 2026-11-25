package com.example.features.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.ui.components.RealRewardTopBar
import com.example.ui.i18n.LocalStrings

@Composable
fun LegalInfoScreen(
    initialSection: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var activeTab by remember(initialSection) {
        mutableIntStateOf(
            when (initialSection) {
                "terms" -> 1
                "privacy" -> 2
                else -> 0
            }
        )
    }

    Scaffold(
        topBar = {
            RealRewardTopBar(
                title = strings.legalTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TabRow(selectedTabIndex = activeTab) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text(strings.aboutUs) })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text(strings.termsConditions) })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text(strings.privacyPolicy) })
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val body = when (activeTab) {
                        0 -> if (language == AppLanguage.BANGLA) {
                            "REAL REWARD হলো বাংলাদেশের জন্য নির্মিত একটি রিয়েল-টাইম অনলাইন মাইক্রোটাস্ক ও রিওয়ার্ড মার্কেটপ্লেস। এখানে নিবন্ধিত সদস্যরা সার্ভে, ওয়েবসাইট ভিজিট, অ্যাপ টেস্টিং ও কুইজ সম্পন্ন করে BDT (৳) রিওয়ার্ড অর্জন করেন।"
                        } else {
                            "REAL REWARD is a real-time Bangladeshi online microtask and rewards marketplace where verified users complete digital tasks to earn BDT (৳) rewards."
                        }
                        1 -> if (language == AppLanguage.BANGLA) {
                            "১. এক-অ্যাকাউন্ট নীতি: প্রতিটি ডিভাইস এবং বাংলাদেশি মোবাইল নম্বরে শুধুমাত্র একটি অ্যাকাউন্ট খোলা যাবে।\n২. ওয়েলকাম বোনাস: প্রথমবার সফল নিবন্ধনে ৳১০ বোনাস প্রদান করা হয়।\n৩. উত্তোলন নীতি: সর্বনিম্ন উত্তোলন ৳৩০০ এবং প্রতি উত্তোলনে ৳১০ প্রসেসিং ফি প্রযোজ্য (মোট ৳৩১০ উপলব্ধ ব্যালেন্স প্রয়োজন)।\n৪. প্রতারণা প্রতিরোধ: ভুয়া প্রুফ বা একাধিক অ্যাকাউন্ট খোলার চেষ্টা করলে অ্যাডমিন অ্যাকাউন্ট স্থগিত করতে পারবেন।"
                        } else {
                            "1. Single-Account Policy: Strictly one account per mobile number and device.\n2. Welcome Bonus: New accounts receive an instant ৳10 registration bonus.\n3. Withdrawals: Minimum payout is ৳300 plus a ৳10 fee (at least ৳310 available balance required).\n4. Fair Play: Fraudulent proofs or multi-account abuse result in permanent account ban."
                        }
                        else -> if (language == AppLanguage.BANGLA) {
                            "REAL REWARD আপনার Google সাইন-ইন তথ্য, মোবাইল নম্বর ও পেমেন্ট অ্যাকাউন্ট নিরাপদ ক্লাউড ফায়ারস্টোরে সংরক্ষণ করে। কোনো ব্যক্তিগত তথ্য তৃতীয় পক্ষের কাছে বিক্রি বা শেয়ার করা হয় না।"
                        } else {
                            "REAL REWARD stores your Google sign-in profile, BD mobile number, and payout numbers securely in encrypted cloud storage solely for task verification and payouts."
                        }
                    }
                    Text(body, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
