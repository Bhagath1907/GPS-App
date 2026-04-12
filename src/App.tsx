/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Smartphone, 
  Bluetooth, 
  Navigation, 
  ShieldCheck, 
  Download, 
  Code2, 
  Info,
  CheckCircle2,
  AlertCircle,
  Terminal
} from 'lucide-react';
import { motion } from 'motion/react';

export default function App() {
  const [activeTab, setActiveTab] = useState('overview');

  const files = [
    { name: 'AndroidManifest.xml', path: 'android-app/app/src/main/AndroidManifest.xml', icon: <ShieldCheck className="w-4 h-4" /> },
    { name: 'MainActivity.kt', path: 'android-app/app/src/main/java/com/example/gpsprovider/MainActivity.kt', icon: <Code2 className="w-4 h-4" /> },
    { name: 'GpsService.kt', path: 'android-app/app/src/main/java/com/example/gpsprovider/GpsService.kt', icon: <Terminal className="w-4 h-4" /> },
    { name: 'NmeaUtils.kt', path: 'android-app/app/src/main/java/com/example/gpsprovider/NmeaUtils.kt', icon: <Navigation className="w-4 h-4" /> },
  ];

  return (
    <div className="min-h-screen bg-[#E4E3E0] text-[#141414] font-sans selection:bg-[#141414] selection:text-[#E4E3E0]">
      {/* Header */}
      <header className="border-b border-[#141414] p-8 flex justify-between items-end">
        <div>
          <h1 className="text-4xl font-bold tracking-tighter uppercase italic font-serif">
            Android GPS Provider
          </h1>
          <p className="text-sm opacity-60 mt-2 font-mono uppercase tracking-widest">
            Bluetooth SPP Server • NMEA 0183 • Aadhaar ECMP Compatible
          </p>
        </div>
        <div className="text-right font-mono text-xs opacity-40 uppercase">
          Project ID: GPS-BT-001<br />
          Build: 2026.04.09
        </div>
      </header>

      <main className="grid grid-cols-12 min-h-[calc(100vh-140px)]">
        {/* Sidebar */}
        <nav className="col-span-3 border-r border-[#141414] p-0">
          <div className="p-4 border-b border-[#141414] bg-[#141414] text-[#E4E3E0]">
            <span className="text-[10px] uppercase tracking-widest font-bold opacity-50">Navigation</span>
          </div>
          <NavItem 
            active={activeTab === 'overview'} 
            onClick={() => setActiveTab('overview')}
            icon={<Info className="w-4 h-4" />}
            label="Overview"
          />
          <NavItem 
            active={activeTab === 'files'} 
            onClick={() => setActiveTab('files')}
            icon={<Code2 className="w-4 h-4" />}
            label="Project Files"
          />
          <NavItem 
            active={activeTab === 'setup'} 
            onClick={() => setActiveTab('setup')}
            icon={<Download className="w-4 h-4" />}
            label="Setup Guide"
          />

          <div className="mt-12 p-6">
            <div className="border border-[#141414] p-4 rounded-sm bg-white/50">
              <h3 className="text-xs font-bold uppercase mb-2 flex items-center gap-2">
                <Bluetooth className="w-3 h-3" /> SPP UUID
              </h3>
              <code className="text-[10px] break-all font-mono opacity-70">
                00001101-0000-1000-8000-00805F9B34FB
              </code>
            </div>
          </div>
        </nav>

        {/* Content */}
        <section className="col-span-9 p-12 overflow-y-auto">
          {activeTab === 'overview' && (
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="space-y-12"
            >
              <div className="grid grid-cols-2 gap-12">
                <div className="space-y-6">
                  <h2 className="text-2xl font-serif italic border-b border-[#141414] pb-2">Core Features</h2>
                  <FeatureItem 
                    title="Fused Location Provider"
                    desc="High-accuracy GPS extraction using Google Play Services."
                  />
                  <FeatureItem 
                    title="NMEA 0183 Generation"
                    desc="Converts raw coordinates to $GPGGA and $GPRMC sentences."
                  />
                  <FeatureItem 
                    title="Bluetooth SPP Server"
                    desc="Emulates a standard serial port for Windows connectivity."
                  />
                  <FeatureItem 
                    title="Foreground Persistence"
                    desc="Runs as a persistent service to prevent OS termination."
                  />
                </div>
                <div className="space-y-6">
                  <h2 className="text-2xl font-serif italic border-b border-[#141414] pb-2">Safety Logic</h2>
                  <div className="p-6 bg-[#141414] text-[#E4E3E0] rounded-sm">
                    <div className="flex items-center gap-3 mb-4">
                      <AlertCircle className="w-5 h-5 text-yellow-400" />
                      <span className="font-mono text-sm uppercase tracking-wider">Accuracy Filter</span>
                    </div>
                    <p className="text-sm opacity-80 leading-relaxed">
                      The application strictly enforces a <span className="text-white font-bold">10-meter accuracy threshold</span>. 
                      Transmission is blocked if GPS horizontal accuracy exceeds this limit, ensuring compliance with Aadhaar ECMP standards.
                    </p>
                  </div>
                  <div className="flex items-center gap-4 p-4 border border-[#141414] rounded-sm">
                    <CheckCircle2 className="w-5 h-5 opacity-40" />
                    <span className="text-sm font-mono uppercase">On-Demand Burst Logic (5 Sentences)</span>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {activeTab === 'files' && (
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="space-y-6"
            >
              <h2 className="text-2xl font-serif italic border-b border-[#141414] pb-2">Generated Project Structure</h2>
              <div className="grid gap-4">
                {files.map((file) => (
                  <div key={file.name} className="flex items-center justify-between p-4 border border-[#141414] hover:bg-[#141414] hover:text-[#E4E3E0] transition-colors group cursor-pointer">
                    <div className="flex items-center gap-4">
                      {file.icon}
                      <div>
                        <div className="text-sm font-bold">{file.name}</div>
                        <div className="text-[10px] font-mono opacity-50 group-hover:opacity-70">{file.path}</div>
                      </div>
                    </div>
                    <Download className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                ))}
              </div>
            </motion.div>
          )}

          {activeTab === 'setup' && (
            <motion.div 
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-8 max-w-2xl"
            >
              <h2 className="text-2xl font-serif italic border-b border-[#141414] pb-2">Implementation Steps</h2>
              <ol className="space-y-8">
                <Step 
                  num="01" 
                  title="Export Project" 
                  desc="Download the project files or copy the generated code into a new Android Studio project (Empty Activity template)." 
                />
                <Step 
                  num="02" 
                  title="Configure Permissions" 
                  desc="Ensure Bluetooth and Location permissions are granted on the device. The app will prompt for these on first launch." 
                />
                <Step 
                  num="03" 
                  title="Pair with Windows" 
                  desc="Go to Windows Bluetooth settings, pair with the Android device. Windows will assign a COM port (e.g., COM3) to the 'Standard Serial over Bluetooth' service." 
                />
                <Step 
                  num="04" 
                  title="Connect ECMP" 
                  desc="In the Aadhaar ECMP application, select the assigned COM port. Press 'TRANSMIT' on the Android app to send the GPS data." 
                />
              </ol>
            </motion.div>
          )}
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-[#141414] p-4 flex justify-between items-center bg-white/30 backdrop-blur-sm">
        <div className="flex items-center gap-6 text-[10px] font-mono uppercase tracking-widest opacity-60">
          <div className="flex items-center gap-2"><Smartphone className="w-3 h-3" /> Android 8.0+</div>
          <div className="flex items-center gap-2"><Bluetooth className="w-3 h-3" /> SPP Profile</div>
          <div className="flex items-center gap-2"><Navigation className="w-3 h-3" /> NMEA 0183</div>
        </div>
        <div className="text-[10px] font-mono opacity-40 italic">
          Designed for Aadhaar Enrollment Client Multi-Platform (ECMP)
        </div>
      </footer>
    </div>
  );
}

function NavItem({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <button 
      onClick={onClick}
      className={`w-full flex items-center gap-4 p-6 text-sm font-mono uppercase tracking-wider transition-all border-b border-[#141414] ${
        active 
          ? 'bg-white font-bold' 
          : 'hover:bg-white/50 opacity-60 hover:opacity-100'
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

function FeatureItem({ title, desc }: { title: string, desc: string }) {
  return (
    <div className="group">
      <h3 className="text-sm font-bold uppercase tracking-tight group-hover:translate-x-1 transition-transform">{title}</h3>
      <p className="text-xs opacity-60 mt-1 leading-relaxed">{desc}</p>
    </div>
  );
}

function Step({ num, title, desc }: { num: string, title: string, desc: string }) {
  return (
    <div className="flex gap-6">
      <span className="text-4xl font-serif italic opacity-20">{num}</span>
      <div>
        <h3 className="text-lg font-bold uppercase tracking-tight">{title}</h3>
        <p className="text-sm opacity-60 mt-1 leading-relaxed">{desc}</p>
      </div>
    </div>
  );
}

