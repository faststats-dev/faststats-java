package dev.faststats;

import java.util.Set;

/**
 * JFR event names.
 * <p>
 * Source: <a href="https://bestsolution-at.github.io/jfr-doc/openjdk-matrix.html">OpenJDK JFR event matrix</a>.
 *
 * @since 0.27.0
 */
public final class FlightEvents {
    // Java 11: https://bestsolution-at.github.io/jfr-doc/openjdk-11.html
    public static final String ACTIVE_RECORDING = "jdk.ActiveRecording";
    public static final String ACTIVE_SETTING = "jdk.ActiveSetting";
    public static final String ALLOCATION_REQUIRING_GC = "jdk.AllocationRequiringGC";
    public static final String BIASED_LOCK_CLASS_REVOCATION = "jdk.BiasedLockClassRevocation";
    public static final String BIASED_LOCK_REVOCATION = "jdk.BiasedLockRevocation";
    public static final String BIASED_LOCK_SELF_REVOCATION = "jdk.BiasedLockSelfRevocation";
    public static final String BOOLEAN_FLAG = "jdk.BooleanFlag";
    public static final String BOOLEAN_FLAG_CHANGED = "jdk.BooleanFlagChanged";
    public static final String CLASS_DEFINE = "jdk.ClassDefine";
    public static final String CLASS_LOAD = "jdk.ClassLoad";
    public static final String CLASS_LOADER_STATISTICS = "jdk.ClassLoaderStatistics";
    public static final String CLASS_LOADING_STATISTICS = "jdk.ClassLoadingStatistics";
    public static final String CLASS_UNLOAD = "jdk.ClassUnload";
    public static final String CODE_CACHE_CONFIGURATION = "jdk.CodeCacheConfiguration";
    public static final String CODE_CACHE_FULL = "jdk.CodeCacheFull";
    public static final String CODE_CACHE_STATISTICS = "jdk.CodeCacheStatistics";
    public static final String CODE_SWEEPER_CONFIGURATION = "jdk.CodeSweeperConfiguration";
    public static final String CODE_SWEEPER_STATISTICS = "jdk.CodeSweeperStatistics";
    public static final String COMPILATION = "jdk.Compilation";
    public static final String COMPILATION_FAILURE = "jdk.CompilationFailure";
    public static final String COMPILER_CONFIGURATION = "jdk.CompilerConfiguration";
    public static final String COMPILER_INLINING = "jdk.CompilerInlining";
    public static final String COMPILER_PHASE = "jdk.CompilerPhase";
    public static final String COMPILER_STATISTICS = "jdk.CompilerStatistics";
    public static final String CONCURRENT_MODE_FAILURE = "jdk.ConcurrentModeFailure";
    public static final String CPU_INFORMATION = "jdk.CPUInformation";
    public static final String CPU_LOAD = "jdk.CPULoad";
    public static final String CPU_TIME_STAMP_COUNTER = "jdk.CPUTimeStampCounter";
    public static final String DATA_LOSS = "jdk.DataLoss";
    public static final String DOUBLE_FLAG = "jdk.DoubleFlag";
    public static final String DOUBLE_FLAG_CHANGED = "jdk.DoubleFlagChanged";
    public static final String DUMP_REASON = "jdk.DumpReason";
    public static final String EVACUATION_FAILED = "jdk.EvacuationFailed";
    public static final String EVACUATION_INFORMATION = "jdk.EvacuationInformation";
    public static final String EXCEPTION_STATISTICS = "jdk.ExceptionStatistics";
    public static final String EXECUTE_VM_OPERATION = "jdk.ExecuteVMOperation";
    public static final String EXECUTION_SAMPLE = "jdk.ExecutionSample";
    public static final String FILE_FORCE = "jdk.FileForce";
    public static final String FILE_READ = "jdk.FileRead";
    public static final String FILE_WRITE = "jdk.FileWrite";
    public static final String G1_ADAPTIVE_IHOP = "jdk.G1AdaptiveIHOP";
    public static final String G1_BASIC_IHOP = "jdk.G1BasicIHOP";
    public static final String G1_EVACUATION_OLD_STATISTICS = "jdk.G1EvacuationOldStatistics";
    public static final String G1_EVACUATION_YOUNG_STATISTICS = "jdk.G1EvacuationYoungStatistics";
    public static final String G1_GARBAGE_COLLECTION = "jdk.G1GarbageCollection";
    public static final String G1_HEAP_REGION_INFORMATION = "jdk.G1HeapRegionInformation";
    public static final String G1_HEAP_REGION_TYPE_CHANGE = "jdk.G1HeapRegionTypeChange";
    public static final String G1_HEAP_SUMMARY = "jdk.G1HeapSummary";
    public static final String G1_MMU = "jdk.G1MMU";
    public static final String GARBAGE_COLLECTION = "jdk.GarbageCollection";
    public static final String GC_CONFIGURATION = "jdk.GCConfiguration";
    public static final String GC_HEAP_CONFIGURATION = "jdk.GCHeapConfiguration";
    public static final String GC_HEAP_SUMMARY = "jdk.GCHeapSummary";
    public static final String GC_PHASE_CONCURRENT = "jdk.GCPhaseConcurrent";
    public static final String GC_PHASE_PAUSE = "jdk.GCPhasePause";
    public static final String GC_PHASE_PAUSE_LEVEL_1 = "jdk.GCPhasePauseLevel1";
    public static final String GC_PHASE_PAUSE_LEVEL_2 = "jdk.GCPhasePauseLevel2";
    public static final String GC_PHASE_PAUSE_LEVEL_3 = "jdk.GCPhasePauseLevel3";
    public static final String GC_PHASE_PAUSE_LEVEL_4 = "jdk.GCPhasePauseLevel4";
    public static final String GC_REFERENCE_STATISTICS = "jdk.GCReferenceStatistics";
    public static final String GC_SURVIVOR_CONFIGURATION = "jdk.GCSurvivorConfiguration";
    public static final String GC_TLAB_CONFIGURATION = "jdk.GCTLABConfiguration";
    public static final String INITIAL_ENVIRONMENT_VARIABLE = "jdk.InitialEnvironmentVariable";
    public static final String INITIAL_SYSTEM_PROPERTY = "jdk.InitialSystemProperty";
    public static final String INT_FLAG = "jdk.IntFlag";
    public static final String INT_FLAG_CHANGED = "jdk.IntFlagChanged";
    public static final String JAVA_ERROR_THROW = "jdk.JavaErrorThrow";
    public static final String JAVA_EXCEPTION_THROW = "jdk.JavaExceptionThrow";
    public static final String JAVA_MONITOR_ENTER = "jdk.JavaMonitorEnter";
    public static final String JAVA_MONITOR_INFLATE = "jdk.JavaMonitorInflate";
    public static final String JAVA_MONITOR_WAIT = "jdk.JavaMonitorWait";
    public static final String JAVA_THREAD_STATISTICS = "jdk.JavaThreadStatistics";
    public static final String JVM_INFORMATION = "jdk.JVMInformation";
    public static final String LONG_FLAG = "jdk.LongFlag";
    public static final String LONG_FLAG_CHANGED = "jdk.LongFlagChanged";
    public static final String METASPACE_ALLOCATION_FAILURE = "jdk.MetaspaceAllocationFailure";
    public static final String METASPACE_CHUNK_FREE_LIST_SUMMARY = "jdk.MetaspaceChunkFreeListSummary";
    public static final String METASPACE_GC_THRESHOLD = "jdk.MetaspaceGCThreshold";
    public static final String METASPACE_OOM = "jdk.MetaspaceOOM";
    public static final String METASPACE_SUMMARY = "jdk.MetaspaceSummary";
    public static final String MODULE_EXPORT = "jdk.ModuleExport";
    public static final String MODULE_REQUIRE = "jdk.ModuleRequire";
    public static final String NATIVE_LIBRARY = "jdk.NativeLibrary";
    public static final String NATIVE_METHOD_SAMPLE = "jdk.NativeMethodSample";
    public static final String NETWORK_UTILIZATION = "jdk.NetworkUtilization";
    public static final String OBJECT_ALLOCATION_IN_NEW_TLAB = "jdk.ObjectAllocationInNewTLAB";
    public static final String OBJECT_ALLOCATION_OUTSIDE_TLAB = "jdk.ObjectAllocationOutsideTLAB";
    public static final String OBJECT_COUNT = "jdk.ObjectCount";
    public static final String OBJECT_COUNT_AFTER_GC = "jdk.ObjectCountAfterGC";
    public static final String OLD_GARBAGE_COLLECTION = "jdk.OldGarbageCollection";
    public static final String OLD_OBJECT_SAMPLE = "jdk.OldObjectSample";
    public static final String OS_INFORMATION = "jdk.OSInformation";
    public static final String PARALLEL_OLD_GARBAGE_COLLECTION = "jdk.ParallelOldGarbageCollection";
    public static final String PHYSICAL_MEMORY = "jdk.PhysicalMemory";
    public static final String PROMOTE_OBJECT_IN_NEW_PLAB = "jdk.PromoteObjectInNewPLAB";
    public static final String PROMOTE_OBJECT_OUTSIDE_PLAB = "jdk.PromoteObjectOutsidePLAB";
    public static final String PROMOTION_FAILED = "jdk.PromotionFailed";
    public static final String PS_HEAP_SUMMARY = "jdk.PSHeapSummary";
    public static final String RESERVED_STACK_ACTIVATION = "jdk.ReservedStackActivation";
    public static final String SAFEPOINT_BEGIN = "jdk.SafepointBegin";
    public static final String SAFEPOINT_CLEANUP = "jdk.SafepointCleanup";
    public static final String SAFEPOINT_CLEANUP_TASK = "jdk.SafepointCleanupTask";
    public static final String SAFEPOINT_END = "jdk.SafepointEnd";
    public static final String SAFEPOINT_STATE_SYNCHRONIZATION = "jdk.SafepointStateSynchronization";
    public static final String SAFEPOINT_WAIT_BLOCKED = "jdk.SafepointWaitBlocked";
    public static final String SECURITY_PROPERTY_MODIFICATION = "jdk.SecurityPropertyModification";
    public static final String SHUTDOWN = "jdk.Shutdown";
    public static final String SOCKET_READ = "jdk.SocketRead";
    public static final String SOCKET_WRITE = "jdk.SocketWrite";
    public static final String STRING_FLAG = "jdk.StringFlag";
    public static final String STRING_FLAG_CHANGED = "jdk.StringFlagChanged";
    public static final String SWEEP_CODE_CACHE = "jdk.SweepCodeCache";
    public static final String SYSTEM_PROCESS = "jdk.SystemProcess";
    public static final String TENURING_DISTRIBUTION = "jdk.TenuringDistribution";
    public static final String THREAD_ALLOCATION_STATISTICS = "jdk.ThreadAllocationStatistics";
    public static final String THREAD_CONTEXT_SWITCH_RATE = "jdk.ThreadContextSwitchRate";
    public static final String THREAD_CPU_LOAD = "jdk.ThreadCPULoad";
    public static final String THREAD_DUMP = "jdk.ThreadDump";
    public static final String THREAD_END = "jdk.ThreadEnd";
    public static final String THREAD_PARK = "jdk.ThreadPark";
    public static final String THREAD_SLEEP = "jdk.ThreadSleep";
    public static final String THREAD_START = "jdk.ThreadStart";
    public static final String TLS_HANDSHAKE = "jdk.TLSHandshake";
    public static final String UNSIGNED_INT_FLAG = "jdk.UnsignedIntFlag";
    public static final String UNSIGNED_INT_FLAG_CHANGED = "jdk.UnsignedIntFlagChanged";
    public static final String UNSIGNED_LONG_FLAG = "jdk.UnsignedLongFlag";
    public static final String UNSIGNED_LONG_FLAG_CHANGED = "jdk.UnsignedLongFlagChanged";
    public static final String VIRTUALIZATION_INFORMATION = "jdk.VirtualizationInformation";
    public static final String X509_CERTIFICATE = "jdk.X509Certificate";
    public static final String X509_VALIDATION = "jdk.X509Validation";
    public static final String YOUNG_GARBAGE_COLLECTION = "jdk.YoungGarbageCollection";
    public static final String YOUNG_GENERATION_CONFIGURATION = "jdk.YoungGenerationConfiguration";
    public static final String Z_PAGE_ALLOCATION = "jdk.ZPageAllocation";
    public static final String Z_STATISTICS_COUNTER = "jdk.ZStatisticsCounter";
    public static final String Z_STATISTICS_SAMPLER = "jdk.ZStatisticsSampler";
    public static final String Z_THREAD_PHASE = "jdk.ZThreadPhase";

    // Java 12: https://bestsolution-at.github.io/jfr-doc/openjdk-12.html
    public static final String GC_PHASE_PARALLEL = "jdk.GCPhaseParallel";

    // Java 13: https://bestsolution-at.github.io/jfr-doc/openjdk-13.html
    public static final String LOADER_CONSTRAINTS_TABLE_STATISTICS = "jdk.LoaderConstraintsTableStatistics";
    public static final String PLACEHOLDER_TABLE_STATISTICS = "jdk.PlaceholderTableStatistics";
    public static final String PROTECTION_DOMAIN_CACHE_TABLE_STATISTICS = "jdk.ProtectionDomainCacheTableStatistics";
    public static final String SHENANDOAH_HEAP_REGION_INFORMATION = "jdk.ShenandoahHeapRegionInformation";
    public static final String SHENANDOAH_HEAP_REGION_STATE_CHANGE = "jdk.ShenandoahHeapRegionStateChange";
    public static final String STRING_TABLE_STATISTICS = "jdk.StringTableStatistics";
    public static final String SYMBOL_TABLE_STATISTICS = "jdk.SymbolTableStatistics";

    // Java 14: https://bestsolution-at.github.io/jfr-doc/openjdk-14.html
    public static final String DEOPTIMIZATION = "jdk.Deoptimization";
    public static final String FLUSH = "jdk.Flush";

    // Java 15: https://bestsolution-at.github.io/jfr-doc/openjdk-15.html
    public static final String CLASS_REDEFINITION = "jdk.ClassRedefinition";
    public static final String DIRECT_BUFFER_STATISTICS = "jdk.DirectBufferStatistics";
    public static final String GC_PHASE_CONCURRENT_LEVEL_1 = "jdk.GCPhaseConcurrentLevel1";
    public static final String HEAP_DUMP = "jdk.HeapDump";
    public static final String PROCESS_START = "jdk.ProcessStart";
    public static final String REDEFINE_CLASSES = "jdk.RedefineClasses";
    public static final String RETRANSFORM_CLASSES = "jdk.RetransformClasses";
    public static final String Z_ALLOCATION_STALL = "jdk.ZAllocationStall";
    public static final String Z_RELOCATION_SET = "jdk.ZRelocationSet";
    public static final String Z_RELOCATION_SET_GROUP = "jdk.ZRelocationSetGroup";
    public static final String Z_UNCOMMIT = "jdk.ZUncommit";
    public static final String Z_UNMAP = "jdk.ZUnmap";

    // Java 16: https://bestsolution-at.github.io/jfr-doc/openjdk-16.html
    public static final String OBJECT_ALLOCATION_SAMPLE = "jdk.ObjectAllocationSample";
    public static final String SYNC_ON_VALUE_BASED_CLASS = "jdk.SyncOnValueBasedClass";

    // Java 17: https://bestsolution-at.github.io/jfr-doc/openjdk-17.html
    public static final String CONTAINER_CONFIGURATION = "jdk.ContainerConfiguration";
    public static final String CONTAINER_CPU_THROTTLING = "jdk.ContainerCPUThrottling";
    public static final String CONTAINER_CPU_USAGE = "jdk.ContainerCPUUsage";
    public static final String CONTAINER_IO_USAGE = "jdk.ContainerIOUsage";
    public static final String CONTAINER_MEMORY_USAGE = "jdk.ContainerMemoryUsage";
    public static final String DESERIALIZATION = "jdk.Deserialization";
    public static final String GC_LOCKER = "jdk.GCLocker";
    public static final String SYSTEM_GC = "jdk.SystemGC";

    // Java 18: https://bestsolution-at.github.io/jfr-doc/openjdk-18.html
    public static final String FINALIZER_STATISTICS = "jdk.FinalizerStatistics";

    // Java 19: https://bestsolution-at.github.io/jfr-doc/openjdk-19.html
    public static final String CONTINUATION_FREEZE = "jdk.ContinuationFreeze";
    public static final String CONTINUATION_FREEZE_OLD = "jdk.ContinuationFreezeOld";
    public static final String CONTINUATION_FREEZE_YOUNG = "jdk.ContinuationFreezeYoung";
    public static final String CONTINUATION_THAW = "jdk.ContinuationThaw";
    public static final String CONTINUATION_THAW_OLD = "jdk.ContinuationThawOld";
    public static final String CONTINUATION_THAW_YOUNG = "jdk.ContinuationThawYoung";
    public static final String VIRTUAL_THREAD_END = "jdk.VirtualThreadEnd";
    public static final String VIRTUAL_THREAD_PINNED = "jdk.VirtualThreadPinned";
    public static final String VIRTUAL_THREAD_START = "jdk.VirtualThreadStart";
    public static final String VIRTUAL_THREAD_SUBMIT_FAILED = "jdk.VirtualThreadSubmitFailed";

    // Java 20: https://bestsolution-at.github.io/jfr-doc/openjdk-20.html
    public static final String CONTINUATION_FREEZE_FAST = "jdk.ContinuationFreezeFast";
    public static final String CONTINUATION_FREEZE_SLOW = "jdk.ContinuationFreezeSlow";
    public static final String CONTINUATION_THAW_FAST = "jdk.ContinuationThawFast";
    public static final String CONTINUATION_THAW_SLOW = "jdk.ContinuationThawSlow";
    public static final String GC_CPU_TIME = "jdk.GCCPUTime";
    public static final String INITIAL_SECURITY_PROPERTY = "jdk.InitialSecurityProperty";
    public static final String JIT_RESTART = "jdk.JITRestart";
    public static final String NATIVE_MEMORY_USAGE = "jdk.NativeMemoryUsage";
    public static final String NATIVE_MEMORY_USAGE_TOTAL = "jdk.NativeMemoryUsageTotal";
    public static final String SECURITY_PROVIDER_SERVICE = "jdk.SecurityProviderService";

    // Java 21: https://bestsolution-at.github.io/jfr-doc/openjdk-21.html
    public static final String GC_HEAP_MEMORY_POOL_USAGE = "jdk.GCHeapMemoryPoolUsage";
    public static final String GC_HEAP_MEMORY_USAGE = "jdk.GCHeapMemoryUsage";
    public static final String GC_PHASE_CONCURRENT_LEVEL_2 = "jdk.GCPhaseConcurrentLevel2";
    public static final String JAVA_AGENT = "jdk.JavaAgent";
    public static final String NATIVE_AGENT = "jdk.NativeAgent";
    public static final String RESIDENT_SET_SIZE = "jdk.ResidentSetSize";
    public static final String Z_OLD_GARBAGE_COLLECTION = "jdk.ZOldGarbageCollection";
    public static final String Z_YOUNG_GARBAGE_COLLECTION = "jdk.ZYoungGarbageCollection";

    // Java 22: https://bestsolution-at.github.io/jfr-doc/openjdk-22.html
    public static final String COMPILER_QUEUE_UTILIZATION = "jdk.CompilerQueueUtilization";
    public static final String DEPRECATED_INVOCATION = "jdk.DeprecatedInvocation";
    public static final String NATIVE_LIBRARY_LOAD = "jdk.NativeLibraryLoad";
    public static final String NATIVE_LIBRARY_UNLOAD = "jdk.NativeLibraryUnload";

    // Java 23: https://bestsolution-at.github.io/jfr-doc/openjdk-23.html
    public static final String SERIALIZATION_MISDECLARATION = "jdk.SerializationMisdeclaration";
    public static final String SWAP_SPACE = "jdk.SwapSpace";

    // Java 24: https://bestsolution-at.github.io/jfr-doc/openjdk-24.html
    public static final String SHENANDOAH_EVACUATION_INFORMATION = "jdk.ShenandoahEvacuationInformation";

    // Java 25: https://bestsolution-at.github.io/jfr-doc/openjdk-25.html
    public static final String CPU_TIME_SAMPLE = "jdk.CPUTimeSample";
    public static final String CPU_TIME_SAMPLES_LOST = "jdk.CPUTimeSamplesLost";
    public static final String JAVA_MONITOR_DEFLATE = "jdk.JavaMonitorDeflate";
    public static final String JAVA_MONITOR_NOTIFY = "jdk.JavaMonitorNotify";
    public static final String JAVA_MONITOR_STATISTICS = "jdk.JavaMonitorStatistics";
    public static final String METHOD_TIMING = "jdk.MethodTiming";
    public static final String METHOD_TRACE = "jdk.MethodTrace";
    public static final String SAFEPOINT_LATENCY = "jdk.SafepointLatency";

    // Java 26: https://bestsolution-at.github.io/jfr-doc/openjdk-26.html
    public static final String FINAL_FIELD_MUTATION = "jdk.FinalFieldMutation";
    public static final String STRING_DEDUPLICATION = "jdk.StringDeduplication";

    /**
     * All known OpenJDK JFR event names.
     * <p>
     * Source: <a href="https://bestsolution-at.github.io/jfr-doc/openjdk-matrix.html">OpenJDK JFR event matrix</a>.
     */
    public static final Set<String> ALL = Set.of(
            ACTIVE_RECORDING,
            ACTIVE_SETTING,
            ALLOCATION_REQUIRING_GC,
            BIASED_LOCK_CLASS_REVOCATION,
            BIASED_LOCK_REVOCATION,
            BIASED_LOCK_SELF_REVOCATION,
            BOOLEAN_FLAG,
            BOOLEAN_FLAG_CHANGED,
            CLASS_DEFINE,
            CLASS_LOAD,
            CLASS_LOADER_STATISTICS,
            CLASS_LOADING_STATISTICS,
            CLASS_UNLOAD,
            CODE_CACHE_CONFIGURATION,
            CODE_CACHE_FULL,
            CODE_CACHE_STATISTICS,
            CODE_SWEEPER_CONFIGURATION,
            CODE_SWEEPER_STATISTICS,
            COMPILATION,
            COMPILATION_FAILURE,
            COMPILER_CONFIGURATION,
            COMPILER_INLINING,
            COMPILER_PHASE,
            COMPILER_STATISTICS,
            CONCURRENT_MODE_FAILURE,
            CPU_INFORMATION,
            CPU_LOAD,
            CPU_TIME_STAMP_COUNTER,
            DATA_LOSS,
            DOUBLE_FLAG,
            DOUBLE_FLAG_CHANGED,
            DUMP_REASON,
            EVACUATION_FAILED,
            EVACUATION_INFORMATION,
            EXCEPTION_STATISTICS,
            EXECUTE_VM_OPERATION,
            EXECUTION_SAMPLE,
            FILE_FORCE,
            FILE_READ,
            FILE_WRITE,
            G1_ADAPTIVE_IHOP,
            G1_BASIC_IHOP,
            G1_EVACUATION_OLD_STATISTICS,
            G1_EVACUATION_YOUNG_STATISTICS,
            G1_GARBAGE_COLLECTION,
            G1_HEAP_REGION_INFORMATION,
            G1_HEAP_REGION_TYPE_CHANGE,
            G1_HEAP_SUMMARY,
            G1_MMU,
            GARBAGE_COLLECTION,
            GC_CONFIGURATION,
            GC_HEAP_CONFIGURATION,
            GC_HEAP_SUMMARY,
            GC_PHASE_CONCURRENT,
            GC_PHASE_PAUSE,
            GC_PHASE_PAUSE_LEVEL_1,
            GC_PHASE_PAUSE_LEVEL_2,
            GC_PHASE_PAUSE_LEVEL_3,
            GC_PHASE_PAUSE_LEVEL_4,
            GC_REFERENCE_STATISTICS,
            GC_SURVIVOR_CONFIGURATION,
            GC_TLAB_CONFIGURATION,
            INITIAL_ENVIRONMENT_VARIABLE,
            INITIAL_SYSTEM_PROPERTY,
            INT_FLAG,
            INT_FLAG_CHANGED,
            JAVA_ERROR_THROW,
            JAVA_EXCEPTION_THROW,
            JAVA_MONITOR_ENTER,
            JAVA_MONITOR_INFLATE,
            JAVA_MONITOR_WAIT,
            JAVA_THREAD_STATISTICS,
            JVM_INFORMATION,
            LONG_FLAG,
            LONG_FLAG_CHANGED,
            METASPACE_ALLOCATION_FAILURE,
            METASPACE_CHUNK_FREE_LIST_SUMMARY,
            METASPACE_GC_THRESHOLD,
            METASPACE_OOM,
            METASPACE_SUMMARY,
            MODULE_EXPORT,
            MODULE_REQUIRE,
            NATIVE_LIBRARY,
            NATIVE_METHOD_SAMPLE,
            NETWORK_UTILIZATION,
            OBJECT_ALLOCATION_IN_NEW_TLAB,
            OBJECT_ALLOCATION_OUTSIDE_TLAB,
            OBJECT_COUNT,
            OBJECT_COUNT_AFTER_GC,
            OLD_GARBAGE_COLLECTION,
            OLD_OBJECT_SAMPLE,
            OS_INFORMATION,
            PARALLEL_OLD_GARBAGE_COLLECTION,
            PHYSICAL_MEMORY,
            PROMOTE_OBJECT_IN_NEW_PLAB,
            PROMOTE_OBJECT_OUTSIDE_PLAB,
            PROMOTION_FAILED,
            PS_HEAP_SUMMARY,
            RESERVED_STACK_ACTIVATION,
            SAFEPOINT_BEGIN,
            SAFEPOINT_CLEANUP,
            SAFEPOINT_CLEANUP_TASK,
            SAFEPOINT_END,
            SAFEPOINT_STATE_SYNCHRONIZATION,
            SAFEPOINT_WAIT_BLOCKED,
            SECURITY_PROPERTY_MODIFICATION,
            SHUTDOWN,
            SOCKET_READ,
            SOCKET_WRITE,
            STRING_FLAG,
            STRING_FLAG_CHANGED,
            SWEEP_CODE_CACHE,
            SYSTEM_PROCESS,
            TENURING_DISTRIBUTION,
            THREAD_ALLOCATION_STATISTICS,
            THREAD_CONTEXT_SWITCH_RATE,
            THREAD_CPU_LOAD,
            THREAD_DUMP,
            THREAD_END,
            THREAD_PARK,
            THREAD_SLEEP,
            THREAD_START,
            TLS_HANDSHAKE,
            UNSIGNED_INT_FLAG,
            UNSIGNED_INT_FLAG_CHANGED,
            UNSIGNED_LONG_FLAG,
            UNSIGNED_LONG_FLAG_CHANGED,
            VIRTUALIZATION_INFORMATION,
            X509_CERTIFICATE,
            X509_VALIDATION,
            YOUNG_GARBAGE_COLLECTION,
            YOUNG_GENERATION_CONFIGURATION,
            Z_PAGE_ALLOCATION,
            Z_STATISTICS_COUNTER,
            Z_STATISTICS_SAMPLER,
            Z_THREAD_PHASE,
            GC_PHASE_PARALLEL,
            LOADER_CONSTRAINTS_TABLE_STATISTICS,
            PLACEHOLDER_TABLE_STATISTICS,
            PROTECTION_DOMAIN_CACHE_TABLE_STATISTICS,
            SHENANDOAH_HEAP_REGION_INFORMATION,
            SHENANDOAH_HEAP_REGION_STATE_CHANGE,
            STRING_TABLE_STATISTICS,
            SYMBOL_TABLE_STATISTICS,
            DEOPTIMIZATION,
            FLUSH,
            CLASS_REDEFINITION,
            DIRECT_BUFFER_STATISTICS,
            GC_PHASE_CONCURRENT_LEVEL_1,
            HEAP_DUMP,
            PROCESS_START,
            REDEFINE_CLASSES,
            RETRANSFORM_CLASSES,
            Z_ALLOCATION_STALL,
            Z_RELOCATION_SET,
            Z_RELOCATION_SET_GROUP,
            Z_UNCOMMIT,
            Z_UNMAP,
            OBJECT_ALLOCATION_SAMPLE,
            SYNC_ON_VALUE_BASED_CLASS,
            CONTAINER_CONFIGURATION,
            CONTAINER_CPU_THROTTLING,
            CONTAINER_CPU_USAGE,
            CONTAINER_IO_USAGE,
            CONTAINER_MEMORY_USAGE,
            DESERIALIZATION,
            GC_LOCKER,
            SYSTEM_GC,
            FINALIZER_STATISTICS,
            CONTINUATION_FREEZE,
            CONTINUATION_FREEZE_OLD,
            CONTINUATION_FREEZE_YOUNG,
            CONTINUATION_THAW,
            CONTINUATION_THAW_OLD,
            CONTINUATION_THAW_YOUNG,
            VIRTUAL_THREAD_END,
            VIRTUAL_THREAD_PINNED,
            VIRTUAL_THREAD_START,
            VIRTUAL_THREAD_SUBMIT_FAILED,
            CONTINUATION_FREEZE_FAST,
            CONTINUATION_FREEZE_SLOW,
            CONTINUATION_THAW_FAST,
            CONTINUATION_THAW_SLOW,
            GC_CPU_TIME,
            INITIAL_SECURITY_PROPERTY,
            JIT_RESTART,
            NATIVE_MEMORY_USAGE,
            NATIVE_MEMORY_USAGE_TOTAL,
            SECURITY_PROVIDER_SERVICE,
            GC_HEAP_MEMORY_POOL_USAGE,
            GC_HEAP_MEMORY_USAGE,
            GC_PHASE_CONCURRENT_LEVEL_2,
            JAVA_AGENT,
            NATIVE_AGENT,
            RESIDENT_SET_SIZE,
            Z_OLD_GARBAGE_COLLECTION,
            Z_YOUNG_GARBAGE_COLLECTION,
            COMPILER_QUEUE_UTILIZATION,
            DEPRECATED_INVOCATION,
            NATIVE_LIBRARY_LOAD,
            NATIVE_LIBRARY_UNLOAD,
            SERIALIZATION_MISDECLARATION,
            SWAP_SPACE,
            SHENANDOAH_EVACUATION_INFORMATION,
            CPU_TIME_SAMPLE,
            CPU_TIME_SAMPLES_LOST,
            JAVA_MONITOR_DEFLATE,
            JAVA_MONITOR_NOTIFY,
            JAVA_MONITOR_STATISTICS,
            METHOD_TIMING,
            METHOD_TRACE,
            SAFEPOINT_LATENCY,
            FINAL_FIELD_MUTATION,
            STRING_DEDUPLICATION
    );

    private FlightEvents() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
