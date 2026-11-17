package com.example.data

import com.example.model.CampusBlock
import com.example.model.CampusRoom
import com.example.model.CampusSignalAnchor
import com.example.model.EdgeType
import com.example.model.FloorPlan
import com.example.model.NavEdge
import com.example.model.NavNode
import com.example.model.RectArea
import com.example.model.RoomCategory
import kotlin.math.hypot

object MceCampusData {

    private const val METERS_PER_UNIT = 0.08f

    // Shared Quadrangle corridor geometry for Main Block (MB)
    private val mbCorridors = listOf(
        RectArea(180f, 155f, 640f, 50f), // North corridor
        RectArea(180f, 500f, 640f, 50f), // South corridor
        RectArea(180f, 155f, 55f, 395f), // West corridor
        RectArea(765f, 155f, 55f, 395f)  // East corridor
    )
    private val mbCourtyard = RectArea(245f, 215f, 510f, 275f)

    // Shared Spine corridor geometry for Academic Block (AB)
    private val abCorridors = listOf(
        RectArea(45f, 310f, 910f, 75f)
    )
    private val ab2Corridors = listOf(
        RectArea(45f, 310f, 910f, 75f),
        RectArea(237f, 225f, 54f, 85f) // Central notice-board & lobby alcove between AB-202 and AB-203
    )

    // ---------------------------------------------------------
    // MAIN BLOCK (MB) - GROUND FLOOR (G)
    // ---------------------------------------------------------
    private val mbGroundRooms = listOf(
        // North Wing (Physics Labs & Staff)
        CampusRoom("mb_g_phys_w", "MB", 0, "PHY-L1", "Physics Lab (West)", RoomCategory.LAB, "North Wing", RectArea(140f, 30f, 220f, 118f), "mb_g_n_w", "Applied Physics & Optics Experimentation Lab", listOf("physics", "optics", "ground")),
        CampusRoom("mb_g_phys_hod", "MB", 0, "PHY-HOD", "Physics HOD Chamber", RoomCategory.OFFICE, "North Wing", RectArea(366f, 30f, 108f, 118f), "mb_g_n_c", "Department of Physics Head Office", listOf("hod", "physics")),
        CampusRoom("mb_g_phys_e", "MB", 0, "PHY-L2", "Physics Lab (East)", RoomCategory.LAB, "North Wing", RectArea(480f, 30f, 200f, 118f), "mb_g_n_e", "Laser & Solid State Physics Laboratory", listOf("physics", "lab")),
        CampusRoom("mb_g_n_staff", "MB", 0, "MB-G14", "Society Staff Room", RoomCategory.OFFICE, "North Wing", RectArea(686f, 30f, 134f, 118f), "mb_g_ne", "Faculty & Society Staff Lounge", listOf("staff", "society")),

        // East Wing (Portico Entrance, ML 01-03, Placement Cell)
        CampusRoom("mb_g_tpo", "MB", 0, "TPO-1", "Training & Placement Office", RoomCategory.OFFICE, "East Wing", RectArea(828f, 30f, 142f, 120f), "mb_g_ne", "Dept. Training & Placement Unit 1 and Placement Officer", listOf("placement", "tpo", "office")),
        CampusRoom("mb_g_ml01", "MB", 0, "ML 01", "Lecture Hall ML 01", RoomCategory.CLASSROOM, "East Wing", RectArea(828f, 156f, 142f, 88f), "mb_g_e_n", "Ground Floor Lecture Room ML 01", listOf("ml01", "classroom")),
        CampusRoom("mb_g_mdr01", "MB", 0, "MDR 01", "Seminar Room MDR 01", RoomCategory.FACILITY, "East Wing", RectArea(828f, 250f, 142f, 82f), "mb_g_e_c", "Multi-Disciplinary Resource Hall MDR 01", listOf("mdr01", "hall")),
        CampusRoom("mb_g_portico", "MB", 0, "PORTICO", "Main Portico Entrance", RoomCategory.ENTRANCE, "East Wing", RectArea(828f, 338f, 142f, 82f), "mb_g_portico_n", "Main Block East Portico & Campus Plaza Entry", listOf("entrance", "portico", "main gate")),
        CampusRoom("mb_g_ml02", "MB", 0, "ML 02", "Lecture Hall ML 02", RoomCategory.CLASSROOM, "East Wing", RectArea(828f, 426f, 142f, 74f), "mb_g_e_s", "Ground Floor Lecture Room ML 02", listOf("ml02")),
        CampusRoom("mb_g_ml03", "MB", 0, "ML 03", "Lecture Hall ML 03", RoomCategory.CLASSROOM, "East Wing", RectArea(828f, 506f, 142f, 70f), "mb_g_se", "Ground Floor Lecture Room ML 03", listOf("ml03")),

        // South Wing (Chemistry Lab, Sports, Interview, Main Stairs & Lift)
        CampusRoom("mb_g_tpo_disc", "MB", 0, "TPO-2", "T&P Discussion & Interview", RoomCategory.OFFICE, "South Wing", RectArea(745f, 582f, 225f, 108f), "mb_g_se", "Placement Discussion & Interview Rooms", listOf("interview", "placement")),
        CampusRoom("mb_g_sports", "MB", 0, "SPORTS", "Dept. of Sports", RoomCategory.OFFICE, "South Wing", RectArea(620f, 558f, 118f, 132f), "mb_g_s_e", "Physical Education & Sports Office", listOf("sports")),
        CampusRoom("mb_g_ml04", "MB", 0, "ML 04", "Lecture Hall ML 04", RoomCategory.CLASSROOM, "South Wing", RectArea(502f, 558f, 112f, 132f), "mb_g_s_c", "Ground Floor Lecture Room ML 04", listOf("ml04")),
        CampusRoom("mb_g_stairs_s", "MB", 0, "STAIRS", "Main Block Central Stairs", RoomCategory.STAIRS, "South Wing", RectArea(430f, 558f, 66f, 132f), "mb_g_stairs_n", "Central Staircase to 1st & 2nd Floor", listOf("stairs", "steps")),
        CampusRoom("mb_g_lift", "MB", 0, "LIFT", "Main Block Elevator", RoomCategory.ELEVATOR, "South Wing", RectArea(368f, 558f, 56f, 132f), "mb_g_lift_n", "Wheelchair-accessible Passenger Lift (G-1-2)", listOf("lift", "elevator", "accessible")),
        CampusRoom("mb_g_chem_lab", "MB", 0, "CHEM-L", "Chemistry Laboratory", RoomCategory.LAB, "South Wing", RectArea(160f, 558f, 202f, 132f), "mb_g_s_w", "Engineering Chemistry Lab & Instruments Room", listOf("chemistry", "lab")),

        // West Wing (Valuation Hall, Exam Conduct, Waiting Area)
        CampusRoom("mb_g_board", "MB", 0, "BOARD", "Conference & Board Room", RoomCategory.FACILITY, "West Wing", RectArea(30f, 540f, 124f, 150f), "mb_g_sw", "Academic Council & Conference Hall", listOf("conference", "board")),
        CampusRoom("mb_g_exam_cond", "MB", 0, "EXAM-C", "Conduct of Examination", RoomCategory.OFFICE, "West Wing", RectArea(30f, 345f, 142f, 188f), "mb_g_w_s", "Examination Conduct & Coding/Decoding Cell", listOf("exam", "coding")),
        CampusRoom("mb_g_valuation", "MB", 0, "VAL-H", "Valuation Hall", RoomCategory.FACILITY, "West Wing", RectArea(30f, 155f, 142f, 184f), "mb_g_w_n", "Central Examination Valuation & Printing Hall", listOf("valuation", "exam")),
        CampusRoom("mb_g_nw_toilets", "MB", 0, "WC-G", "Restrooms (NW)", RoomCategory.AMENITY, "North-West", RectArea(30f, 30f, 104f, 118f), "mb_g_nw", "Ground Floor Restrooms & Drinking Water", listOf("toilet", "restroom", "washroom"))
    )

    // ---------------------------------------------------------
    // MAIN BLOCK (MB) - FIRST FLOOR (1)
    // ---------------------------------------------------------
    private val mbFirstRooms = listOf(
        // North Wing (Classrooms & Maths HOD)
        CampusRoom("mb_1_nw_wc", "MB", 1, "WC-1F", "Restrooms (1F NW)", RoomCategory.AMENITY, "North-West", RectArea(30f, 30f, 104f, 118f), "mb_1_nw", "First Floor Restrooms", listOf("toilet", "restroom")),
        CampusRoom("mb_1_cr101", "MB", 1, "MB 101", "Classroom 101", RoomCategory.CLASSROOM, "North Wing", RectArea(140f, 30f, 145f, 118f), "mb_1_n_w", "First Floor Lecture Hall 101", listOf("101", "class")),
        CampusRoom("mb_1_cr102", "MB", 1, "MB 102", "Classroom 102", RoomCategory.CLASSROOM, "North Wing", RectArea(291f, 30f, 145f, 118f), "mb_1_n_c", "First Floor Lecture Hall 102", listOf("102", "class")),
        CampusRoom("mb_1_cr103", "MB", 1, "MB 103", "Classroom 103", RoomCategory.CLASSROOM, "North Wing", RectArea(442f, 30f, 145f, 118f), "mb_1_n_e", "First Floor Lecture Hall 103", listOf("103", "class")),
        CampusRoom("mb_1_maths_hod", "MB", 1, "MATH-HOD", "Maths HOD & Staff", RoomCategory.OFFICE, "North Wing", RectArea(593f, 30f, 227f, 118f), "mb_1_ne", "Mathematics HOD Chamber & Faculty Room", listOf("maths", "hod")),

        // East Wing (MDR 02, SWO, Exam)
        CampusRoom("mb_1_mdr02", "MB", 1, "MDR 02", "Hall MDR 02", RoomCategory.FACILITY, "East Wing", RectArea(828f, 155f, 142f, 145f), "mb_1_e_n", "First Floor Resource Hall MDR 02", listOf("mdr02")),
        CampusRoom("mb_1_swo", "MB", 1, "SWO", "Student Welfare Office", RoomCategory.OFFICE, "East Wing", RectArea(828f, 306f, 142f, 125f), "mb_1_e_c", "Student Welfare & Scholarships Office", listOf("swo", "welfare")),
        CampusRoom("mb_1_exam_sec", "MB", 1, "EXAM-1F", "Examination Section", RoomCategory.OFFICE, "East Wing", RectArea(828f, 437f, 142f, 138f), "mb_1_se", "Main Examination Records & Strong Room", listOf("exam", "records")),

        // South Wing (Principal, Vice Principal, Reception, Stairs/Lift, ML 06-07, CAM/CAD)
        CampusRoom("mb_1_office", "MB", 1, "OFFICE", "Principal & Admin Office", RoomCategory.OFFICE, "South Wing", RectArea(620f, 558f, 200f, 132f), "mb_1_s_e", "Principal Room, Principal P.A. & Administrative Office", listOf("principal", "admin", "office")),
        CampusRoom("mb_1_vp", "MB", 1, "VP-RM", "Vice Principal & Reception", RoomCategory.OFFICE, "South Wing", RectArea(502f, 558f, 112f, 132f), "mb_1_s_c", "Vice Principal Chamber & First Floor Reception", listOf("vice principal", "reception")),
        CampusRoom("mb_1_stairs_s", "MB", 1, "STAIRS", "Main Block Stairs (1F)", RoomCategory.STAIRS, "South Wing", RectArea(430f, 558f, 66f, 132f), "mb_1_stairs_n", "Central Staircase Landing (G - 1 - 2)", listOf("stairs")),
        CampusRoom("mb_1_lift", "MB", 1, "LIFT", "Main Block Lift (1F)", RoomCategory.ELEVATOR, "South Wing", RectArea(368f, 558f, 56f, 132f), "mb_1_lift_n", "Accessible Elevator Landing (1st Floor)", listOf("lift", "elevator")),
        CampusRoom("mb_1_ml06", "MB", 1, "ML 06", "Hall ML 06 & ML 07", RoomCategory.CLASSROOM, "South Wing", RectArea(215f, 558f, 147f, 132f), "mb_1_s_w", "First Floor Lecture Halls ML 06 & ML 07", listOf("ml06", "ml07")),
        CampusRoom("mb_1_cad_lab", "MB", 1, "CAD-LAB", "CAM/CAD & Robotics Lab", RoomCategory.LAB, "South-West", RectArea(30f, 558f, 179f, 132f), "mb_1_sw", "CAM/CAD Design & Robotics Laboratory", listOf("cad", "cam", "robotics", "lab")),

        // West Wing (Seminar Hall, HOD, Dept Library, Research Centre)
        CampusRoom("mb_1_seminar", "MB", 1, "SEM-1F", "Main Seminar Hall", RoomCategory.FACILITY, "West Wing", RectArea(30f, 368f, 142f, 182f), "mb_1_w_s", "Departmental Seminar Hall & Auditorium", listOf("seminar", "hall")),
        CampusRoom("mb_1_library", "MB", 1, "LIB-1F", "Department Library", RoomCategory.FACILITY, "West Wing", RectArea(30f, 250f, 142f, 112f), "mb_1_w_n", "Department Reference Library & Staff Cabins", listOf("library", "books")),
        CampusRoom("mb_1_research", "MB", 1, "RES-CTR", "Research Centre", RoomCategory.LAB, "West Wing", RectArea(30f, 155f, 142f, 89f), "mb_1_nw", "R&D Research Scholars & Innovation Centre", listOf("research", "phd"))
    )

    // ---------------------------------------------------------
    // MAIN BLOCK (MB) - SECOND FLOOR (2) - From Video 1 (MB 202..205)
    // ---------------------------------------------------------
    private val mbSecondRooms = listOf(
        CampusRoom("mb_2_201", "MB", 2, "MB 201", "CS Lecture Room MB 201", RoomCategory.CLASSROOM, "North Wing", RectArea(140f, 30f, 155f, 118f), "mb_2_n_w", "Second Floor Lecture Hall MB 201", listOf("mb 201", "201")),
        CampusRoom("mb_2_202", "MB", 2, "MB 202", "Programming Lab (CoE)", RoomCategory.LAB, "North Wing", RectArea(301f, 30f, 190f, 118f), "mb_2_n_c", "Center of Excellence for Youth Empowerment & Programming Lab (MB 202)", listOf("mb 202", "202", "programming", "coe", "youth empowerment")),
        CampusRoom("mb_2_203", "MB", 2, "MB 203", "Make in MCE & MargaDarsaka", RoomCategory.LAB, "North Wing", RectArea(497f, 30f, 185f, 118f), "mb_2_n_e", "Make in MCE Centre, MargaDarsaka & MCE Alumni Association (MB 203)", listOf("mb 203", "203", "make in mce", "margadarsaka", "alumni")),
        CampusRoom("mb_2_204", "MB", 2, "MB 204", "AI & Computing Lab MB 204", RoomCategory.LAB, "North Wing", RectArea(688f, 30f, 132f, 118f), "mb_2_ne", "Second Floor Systems & Computing Lab MB 204", listOf("mb 204", "204", "lab")),

        CampusRoom("mb_2_205", "MB", 2, "MB 205", "Project & Lab Room MB 205", RoomCategory.LAB, "East Wing", RectArea(828f, 155f, 142f, 155f), "mb_2_e_n", "Second Floor Project & Simulation Lab MB 205", listOf("mb 205", "205", "project")),
        CampusRoom("mb_2_206", "MB", 2, "MB 206", "Design Thinking Studio", RoomCategory.FACILITY, "East Wing", RectArea(828f, 316f, 142f, 155f), "mb_2_e_c", "Collaborative Innovation & Design Studio", listOf("mb 206", "206", "studio")),
        CampusRoom("mb_2_faculty", "MB", 2, "MB-2F-FAC", "2F Faculty Wing", RoomCategory.OFFICE, "East Wing", RectArea(828f, 477f, 142f, 100f), "mb_2_se", "Second Floor Faculty Cabins", listOf("faculty", "cabin")),

        CampusRoom("mb_2_stairs_s", "MB", 2, "STAIRS", "Main Block Stairs (2F)", RoomCategory.STAIRS, "South Wing", RectArea(430f, 558f, 66f, 132f), "mb_2_stairs_n", "Central Staircase to 1st & Ground Floor", listOf("stairs")),
        CampusRoom("mb_2_lift", "MB", 2, "LIFT", "Main Block Lift (2F)", RoomCategory.ELEVATOR, "South Wing", RectArea(368f, 558f, 56f, 132f), "mb_2_lift_n", "Accessible Elevator Landing (2nd Floor)", listOf("lift", "elevator")),
        CampusRoom("mb_2_lounge", "MB", 2, "MB-2F-LH", "2nd Floor Student Lounge", RoomCategory.FACILITY, "South Wing", RectArea(502f, 558f, 220f, 132f), "mb_2_s_e", "Open Study & Collaboration Zone", listOf("study", "lounge")),
        CampusRoom("mb_2_wc", "MB", 2, "WC-2F", "Restrooms (2F)", RoomCategory.AMENITY, "West Wing", RectArea(30f, 30f, 104f, 118f), "mb_2_nw", "Second Floor Restrooms & Water Fountain", listOf("toilet", "restroom"))
    )

    // ---------------------------------------------------------
    // ACADEMIC BLOCK (AB) - GROUND, 1ST, AND 2ND FLOORS (Video 2: AB-201..AB-213)
    // ---------------------------------------------------------
    private val abGroundRooms = listOf(
        CampusRoom("ab_g_atrium", "AB", 0, "AB-ENTRY", "AB Atrium Entrance", RoomCategory.ENTRANCE, "West Atrium", RectArea(45f, 165f, 145f, 140f), "ab_g_w", "Academic Block Ground Floor Entrance & Helpdesk", listOf("entrance", "atrium", "academic block")),
        CampusRoom("ab_g_001", "AB", 0, "AB-001", "Lecture Theatre AB-001", RoomCategory.CLASSROOM, "North Corridor", RectArea(200f, 165f, 180f, 140f), "ab_g_c1", "Tiered Multimedia Lecture Hall AB-001", listOf("ab-001", "001")),
        CampusRoom("ab_g_002", "AB", 0, "AB-002", "Data Science Lab AB-002", RoomCategory.LAB, "North Corridor", RectArea(390f, 165f, 190f, 140f), "ab_g_c2", "High-Performance Computing & Data Science Lab", listOf("ab-002", "002", "data science", "lab")),
        CampusRoom("ab_g_003", "AB", 0, "AB-003", "Embedded & VLSI Lab", RoomCategory.LAB, "North Corridor", RectArea(590f, 165f, 190f, 140f), "ab_g_c3", "Microcontroller, Embedded & VLSI Lab AB-003", listOf("ab-003", "003", "vlsi", "embedded")),
        CampusRoom("ab_g_004", "AB", 0, "AB-004", "AB Faculty Lounge", RoomCategory.OFFICE, "South Corridor", RectArea(200f, 392f, 220f, 140f), "ab_g_c1", "Academic Block Faculty & Mentoring Rooms", listOf("faculty", "office")),
        CampusRoom("ab_g_005", "AB", 0, "AB-005", "Network Systems Lab", RoomCategory.LAB, "South Corridor", RectArea(430f, 392f, 220f, 140f), "ab_g_c3", "Computer Networks & Cloud Lab AB-005", listOf("networks", "cloud", "lab")),
        CampusRoom("ab_g_wc", "AB", 0, "AB-WC", "AB Ground Restrooms", RoomCategory.AMENITY, "South Corridor", RectArea(660f, 392f, 120f, 140f), "ab_g_c4", "Ground Floor Restrooms", listOf("toilet", "restroom")),
        CampusRoom("ab_g_lift", "AB", 0, "AB-LIFT", "AB Elevator (G)", RoomCategory.ELEVATOR, "East Core", RectArea(795f, 165f, 75f, 140f), "ab_g_e", "Academic Block Accessible Lift", listOf("lift", "elevator")),
        CampusRoom("ab_g_stairs", "AB", 0, "AB-STAIRS", "AB East Stairs (G)", RoomCategory.STAIRS, "East Core", RectArea(876f, 165f, 79f, 140f), "ab_g_e", "Academic Block Main Staircase", listOf("stairs"))
    )

    private val abFirstRooms = listOf(
        CampusRoom("ab_1_101", "AB", 1, "AB-101", "Classroom AB-101", RoomCategory.CLASSROOM, "North Corridor", RectArea(55f, 165f, 145f, 140f), "ab_1_w", "First Floor Classroom AB-101", listOf("101")),
        CampusRoom("ab_1_102", "AB", 1, "AB-102", "Classroom AB-102", RoomCategory.CLASSROOM, "North Corridor", RectArea(210f, 165f, 145f, 140f), "ab_1_c1", "First Floor Classroom AB-102", listOf("102")),
        CampusRoom("ab_1_103", "AB", 1, "AB-103", "Software Engg. Lab AB-103", RoomCategory.LAB, "North Corridor", RectArea(365f, 165f, 160f, 140f), "ab_1_c2", "Software Engineering & DBMS Lab", listOf("103", "lab", "dbms")),
        CampusRoom("ab_1_104", "AB", 1, "AB-104", "Classroom AB-104", RoomCategory.CLASSROOM, "North Corridor", RectArea(535f, 165f, 150f, 140f), "ab_1_c3", "First Floor Classroom AB-104", listOf("104")),
        CampusRoom("ab_1_105", "AB", 1, "AB-105", "AB-1F Seminar Room", RoomCategory.FACILITY, "South Corridor", RectArea(55f, 392f, 200f, 140f), "ab_1_w", "Department Colloquium & Seminar Hall", listOf("105", "seminar")),
        CampusRoom("ab_1_106", "AB", 1, "AB-106", "Classroom AB-106", RoomCategory.CLASSROOM, "South Corridor", RectArea(265f, 392f, 165f, 140f), "ab_1_c1", "First Floor Classroom AB-106", listOf("106")),
        CampusRoom("ab_1_108", "AB", 1, "AB-108", "AI & Vision Lab AB-108", RoomCategory.LAB, "South Corridor", RectArea(440f, 392f, 180f, 140f), "ab_1_c3", "Computer Vision & Deep Learning Lab", listOf("108", "ai", "vision")),
        CampusRoom("ab_1_109", "AB", 1, "AB-109", "Faculty Cabins AB-109", RoomCategory.OFFICE, "South Corridor", RectArea(630f, 392f, 145f, 140f), "ab_1_c4", "Academic Block 1F Faculty Offices", listOf("109", "faculty")),
        CampusRoom("ab_1_lift", "AB", 1, "AB-LIFT", "AB Elevator (1F)", RoomCategory.ELEVATOR, "East Core", RectArea(795f, 165f, 75f, 140f), "ab_1_e", "Academic Block Elevator Landing (1F)", listOf("lift")),
        CampusRoom("ab_1_stairs", "AB", 1, "AB-STAIRS", "AB East Stairs (1F)", RoomCategory.STAIRS, "East Core", RectArea(876f, 165f, 79f, 140f), "ab_1_e", "Academic Block Staircase (1F)", listOf("stairs"))
    )

    // Exact classroom sequence from walkthrough video: AB-201..207 (Left/North) and AB-208..212 (Right/South)
    private val abSecondRooms = listOf(
        CampusRoom("ab_2_201", "AB", 2, "AB-201", "Classroom AB-201", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(45f, 165f, 90f, 140f), "ab_2_w", "Second Floor Lecture Classroom AB-201", listOf("ab-201", "ab 201", "201", "classroom")),
        CampusRoom("ab_2_202", "AB", 2, "AB-202", "Classroom AB-202", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(141f, 165f, 90f, 140f), "ab_2_n1", "Second Floor Lecture Classroom AB-202", listOf("ab-202", "ab 202", "202", "classroom")),
        CampusRoom("ab_2_203", "AB", 2, "AB-203", "Classroom AB-203", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(296f, 165f, 90f, 140f), "ab_2_n2", "Second Floor Lecture Classroom AB-203 (after notice board bay)", listOf("ab-203", "ab 203", "203", "classroom")),
        CampusRoom("ab_2_204", "AB", 2, "AB-204", "Classroom AB-204", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(392f, 165f, 90f, 140f), "ab_2_n3", "Second Floor Lecture Classroom AB-204", listOf("ab-204", "ab 204", "204", "classroom")),
        CampusRoom("ab_2_205", "AB", 2, "AB-205", "Classroom AB-205", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(488f, 165f, 90f, 140f), "ab_2_n4", "Second Floor Lecture Classroom AB-205", listOf("ab-205", "ab 205", "205", "classroom")),
        CampusRoom("ab_2_206", "AB", 2, "AB-206", "Classroom AB-206", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(584f, 165f, 90f, 140f), "ab_2_n5", "Second Floor Lecture Classroom AB-206", listOf("ab-206", "ab 206", "206", "classroom")),
        CampusRoom("ab_2_207", "AB", 2, "AB-207", "Classroom AB-207", RoomCategory.CLASSROOM, "Left Wing (North)", RectArea(680f, 165f, 95f, 140f), "ab_2_n6", "Second Floor Lecture Classroom AB-207 (opposite AB-212 near stairwell)", listOf("ab-207", "ab 207", "207", "classroom")),

        CampusRoom("ab_2_208", "AB", 2, "AB-208", "Classroom AB-208", RoomCategory.CLASSROOM, "Right Wing (South)", RectArea(141f, 392f, 100f, 140f), "ab_2_n1", "Second Floor Lecture Classroom AB-208", listOf("ab-208", "ab 208", "208", "classroom")),
        CampusRoom("ab_2_209", "AB", 2, "AB-209", "Classroom AB-209", RoomCategory.CLASSROOM, "Right Wing (South)", RectArea(248f, 392f, 100f, 140f), "ab_2_bay", "Second Floor Lecture Classroom AB-209 (before central lobby)", listOf("ab-209", "ab 209", "209", "classroom")),
        CampusRoom("ab_2_210", "AB", 2, "AB-210", "Classroom AB-210", RoomCategory.CLASSROOM, "Right Wing (South)", RectArea(425f, 392f, 105f, 140f), "ab_2_n3", "Second Floor Lecture Classroom AB-210", listOf("ab-210", "ab 210", "210", "classroom")),
        CampusRoom("ab_2_211", "AB", 2, "AB-211", "Classroom AB-211", RoomCategory.CLASSROOM, "Right Wing (South)", RectArea(537f, 392f, 105f, 140f), "ab_2_n4", "Second Floor Lecture Classroom AB-211", listOf("ab-211", "ab 211", "211", "classroom")),
        CampusRoom("ab_2_212", "AB", 2, "AB-212", "Classroom AB-212", RoomCategory.CLASSROOM, "Right Wing (South)", RectArea(649f, 392f, 126f, 140f), "ab_2_n6", "Second Floor Lecture Classroom AB-212 (next to East stairwell)", listOf("ab-212", "ab 212", "212", "classroom")),

        CampusRoom("ab_2_lift", "AB", 2, "AB-LIFT", "AB Elevator (2F)", RoomCategory.ELEVATOR, "East Core", RectArea(795f, 165f, 75f, 140f), "ab_2_e", "Academic Block Accessible Lift (2F)", listOf("lift")),
        CampusRoom("ab_2_stairs", "AB", 2, "AB-STAIRS", "AB East Stairs (2F)", RoomCategory.STAIRS, "East Core", RectArea(876f, 165f, 79f, 140f), "ab_2_e", "Academic Block Main Staircase (2F)", listOf("stairs"))
    )

    val blocks: List<CampusBlock> = listOf(
        CampusBlock(
            id = "MB",
            code = "MB",
            name = "Main Block",
            subtitle = "Admin, Core Sciences, Labs & CoE Wing",
            accentHex = 0xFF163E82,
            description = "Central quadrangle complex housing Principal & Exam Offices, Physics & Chemistry Labs, Valuation Hall, Seminar Hall, and 2F Centers of Excellence (MB 201–206).",
            floors = listOf(
                FloorPlan("MB", 0, "G", "Ground Floor — Labs, Portico & Valuation", hasCourtyard = true, courtyardRect = mbCourtyard, corridors = mbCorridors, rooms = mbGroundRooms),
                FloorPlan("MB", 1, "1", "First Floor — Admin, Library & Classrooms", hasCourtyard = true, courtyardRect = mbCourtyard, corridors = mbCorridors, rooms = mbFirstRooms),
                FloorPlan("MB", 2, "2", "Second Floor — MB 201–206 & CoE Labs", hasCourtyard = true, courtyardRect = mbCourtyard, corridors = mbCorridors, rooms = mbSecondRooms)
            )
        ),
        CampusBlock(
            id = "AB",
            code = "AB",
            name = "Academic Block",
            subtitle = "Computing Labs & Classrooms (AB 001–212)",
            accentHex = 0xFF0D9488,
            description = "Three-story spine-corridor academic wing connected via the Main Plaza walkway, featuring lecture classrooms AB-201 to AB-212 and computing laboratories.",
            floors = listOf(
                FloorPlan("AB", 0, "G", "Ground Floor — Atrium & Computing Labs", hasCourtyard = false, corridors = abCorridors, rooms = abGroundRooms),
                FloorPlan("AB", 1, "1", "First Floor — AB-101 to AB-109", hasCourtyard = false, corridors = abCorridors, rooms = abFirstRooms),
                FloorPlan("AB", 2, "2", "Second Floor — Classrooms AB-201 to AB-212", hasCourtyard = false, corridors = ab2Corridors, rooms = abSecondRooms)
            )
        )
    )

    val allRooms: List<CampusRoom> = blocks.flatMap { it.floors }.flatMap { it.rooms }

    fun getBlock(blockId: String): CampusBlock? = blocks.find { it.id == blockId }

    fun getFloor(blockId: String, floorNumber: Int): FloorPlan? =
        getBlock(blockId)?.floors?.find { it.floorNumber == floorNumber }

    fun getRoom(roomId: String): CampusRoom? = allRooms.find { it.id == roomId }

    // ---------------------------------------------------------
    // NAVIGATION GRAPH NODES
    // ---------------------------------------------------------
    val nodes: List<NavNode> = buildList {
        // Main Block Quadrangle Corridor Nodes across floors 0, 1, 2
        listOf(0, 1, 2).forEach { fl ->
            val p = "mb_${if (fl == 0) "g" else fl.toString()}"
            add(NavNode("${p}_nw", "MB", fl, 207f, 180f, "NW Corner"))
            add(NavNode("${p}_n_w", "MB", fl, 290f, 180f, "North Corridor West"))
            add(NavNode("${p}_n_c", "MB", fl, 430f, 180f, "North Corridor Center"))
            add(NavNode("${p}_n_e", "MB", fl, 600f, 180f, "North Corridor East"))
            add(NavNode("${p}_ne", "MB", fl, 792f, 180f, "NE Corner"))

            add(NavNode("${p}_e_n", "MB", fl, 792f, 230f, "East Corridor North"))
            add(NavNode("${p}_e_c", "MB", fl, 792f, 340f, "East Corridor Center"))
            if (fl == 0) {
                add(NavNode("${p}_portico_n", "MB", 0, 792f, 380f, "Main Portico Entrance", isEntrance = true))
            }
            add(NavNode("${p}_e_s", "MB", fl, 792f, 455f, "East Corridor South"))
            add(NavNode("${p}_se", "MB", fl, 792f, 525f, "SE Corner"))

            add(NavNode("${p}_s_e", "MB", fl, 650f, 525f, "South Corridor East"))
            add(NavNode("${p}_s_c", "MB", fl, 550f, 525f, "South Corridor Center"))
            add(NavNode("${p}_stairs_n", "MB", fl, 463f, 525f, "Central Stairs", isStairs = true))
            add(NavNode("${p}_lift_n", "MB", fl, 396f, 525f, "Main Elevator", isElevator = true))
            add(NavNode("${p}_s_w", "MB", fl, 285f, 525f, "South Corridor West"))
            add(NavNode("${p}_sw", "MB", fl, 207f, 525f, "SW Corner"))

            add(NavNode("${p}_w_s", "MB", fl, 207f, 425f, "West Corridor South"))
            add(NavNode("${p}_w_n", "MB", fl, 207f, 285f, "West Corridor North"))
        }

        // Academic Block Spine Corridor Nodes across floors 0, 1, 2
        // Floor 0
        add(NavNode("ab_g_w", "AB", 0, 115f, 348f, "AB Atrium Entrance", isEntrance = true))
        add(NavNode("ab_g_c1", "AB", 0, 290f, 348f, "AB Ground West"))
        add(NavNode("ab_g_c2", "AB", 0, 480f, 348f, "AB Ground Center"))
        add(NavNode("ab_g_c3", "AB", 0, 620f, 348f, "AB Ground East"))
        add(NavNode("ab_g_c4", "AB", 0, 725f, 348f, "AB Ground Hall"))
        add(NavNode("ab_g_e", "AB", 0, 865f, 348f, "AB Lift & Stairs (G)", isStairs = true, isElevator = true))

        // Floor 1
        add(NavNode("ab_1_w", "AB", 1, 130f, 348f, "AB 1F West"))
        add(NavNode("ab_1_c1", "AB", 1, 295f, 348f, "AB 1F Mid-West"))
        add(NavNode("ab_1_c2", "AB", 1, 445f, 348f, "AB 1F Center"))
        add(NavNode("ab_1_c3", "AB", 1, 590f, 348f, "AB 1F Mid-East"))
        add(NavNode("ab_1_c4", "AB", 1, 710f, 348f, "AB 1F East"))
        add(NavNode("ab_1_e", "AB", 1, 865f, 348f, "AB Lift & Stairs (1F)", isStairs = true, isElevator = true))

        // Floor 2 (Walkthrough corridor: AB-201..207 North, AB-208..212 South)
        add(NavNode("ab_2_w", "AB", 2, 90f, 348f, "AB-201 Entrance"))
        add(NavNode("ab_2_n1", "AB", 2, 186f, 348f, "AB-202 / AB-208 Junction"))
        add(NavNode("ab_2_bay", "AB", 2, 268f, 348f, "Central Notice Board & AB-209"))
        add(NavNode("ab_2_n2", "AB", 2, 341f, 348f, "AB-203 Doorway"))
        add(NavNode("ab_2_n3", "AB", 2, 437f, 348f, "AB-204 / AB-210 Junction"))
        add(NavNode("ab_2_n4", "AB", 2, 533f, 348f, "AB-205 / AB-211 Junction"))
        add(NavNode("ab_2_n5", "AB", 2, 629f, 348f, "AB-206 Doorway"))
        add(NavNode("ab_2_n6", "AB", 2, 725f, 348f, "AB-207 / AB-212 Junction"))
        add(NavNode("ab_2_e", "AB", 2, 865f, 348f, "AB Lift & Stairs (2F)", isStairs = true, isElevator = true))
    }

    private val nodeMap = nodes.associateBy { it.id }

    fun getNode(nodeId: String): NavNode? = nodeMap[nodeId]

    private fun link(edges: MutableList<NavEdge>, a: String, b: String, type: EdgeType = EdgeType.CORRIDOR, overrideMeters: Float? = null) {
        val na = nodeMap[a] ?: return
        val nb = nodeMap[b] ?: return
        val dist = overrideMeters ?: (hypot(na.x - nb.x, na.y - nb.y) * METERS_PER_UNIT).coerceAtLeast(2f)
        val accessible = type != EdgeType.STAIRS
        edges.add(NavEdge(a, b, dist, type, accessible))
        edges.add(NavEdge(b, a, dist, type, accessible))
    }

    val edges: List<NavEdge> = buildList {
        // Main Block horizontal loops on each floor
        listOf(0, 1, 2).forEach { fl ->
            val p = "mb_${if (fl == 0) "g" else fl.toString()}"
            link(this, "${p}_nw", "${p}_n_w")
            link(this, "${p}_n_w", "${p}_n_c")
            link(this, "${p}_n_c", "${p}_n_e")
            link(this, "${p}_n_e", "${p}_ne")

            link(this, "${p}_ne", "${p}_e_n")
            link(this, "${p}_e_n", "${p}_e_c")
            if (fl == 0) {
                link(this, "${p}_e_c", "${p}_portico_n")
                link(this, "${p}_portico_n", "${p}_e_s")
            } else {
                link(this, "${p}_e_c", "${p}_e_s")
            }
            link(this, "${p}_e_s", "${p}_se")

            link(this, "${p}_se", "${p}_s_e")
            link(this, "${p}_s_e", "${p}_s_c")
            link(this, "${p}_s_c", "${p}_stairs_n")
            link(this, "${p}_stairs_n", "${p}_lift_n")
            link(this, "${p}_lift_n", "${p}_s_w")
            link(this, "${p}_s_w", "${p}_sw")

            link(this, "${p}_sw", "${p}_w_s")
            link(this, "${p}_w_s", "${p}_w_n")
            link(this, "${p}_w_n", "${p}_nw")
        }

        // Main Block vertical connections (Stairs & Lift)
        link(this, "mb_g_stairs_n", "mb_1_stairs_n", EdgeType.STAIRS, 10f)
        link(this, "mb_1_stairs_n", "mb_2_stairs_n", EdgeType.STAIRS, 10f)
        link(this, "mb_g_lift_n", "mb_1_lift_n", EdgeType.ELEVATOR, 6f)
        link(this, "mb_1_lift_n", "mb_2_lift_n", EdgeType.ELEVATOR, 6f)

        // Academic Block spine corridors
        link(this, "ab_g_w", "ab_g_c1")
        link(this, "ab_g_c1", "ab_g_c2")
        link(this, "ab_g_c2", "ab_g_c3")
        link(this, "ab_g_c3", "ab_g_c4")
        link(this, "ab_g_c4", "ab_g_e")

        link(this, "ab_1_w", "ab_1_c1")
        link(this, "ab_1_c1", "ab_1_c2")
        link(this, "ab_1_c2", "ab_1_c3")
        link(this, "ab_1_c3", "ab_1_c4")
        link(this, "ab_1_c4", "ab_1_e")

        link(this, "ab_2_w", "ab_2_n1")
        link(this, "ab_2_n1", "ab_2_bay")
        link(this, "ab_2_bay", "ab_2_n2")
        link(this, "ab_2_n2", "ab_2_n3")
        link(this, "ab_2_n3", "ab_2_n4")
        link(this, "ab_2_n4", "ab_2_n5")
        link(this, "ab_2_n5", "ab_2_n6")
        link(this, "ab_2_n6", "ab_2_e")

        // Academic Block vertical connections (Stairs & Elevator)
        link(this, "ab_g_e", "ab_1_e", EdgeType.STAIRS, 9f)
        link(this, "ab_1_e", "ab_2_e", EdgeType.STAIRS, 9f)
        link(this, "ab_g_e", "ab_1_e", EdgeType.ELEVATOR, 6f)
        link(this, "ab_1_e", "ab_2_e", EdgeType.ELEVATOR, 6f)

        // Outdoor Campus Plaza Walkway between Main Block Portico <-> Academic Block Atrium
        link(this, "mb_g_portico_n", "ab_g_w", EdgeType.OUTDOOR_WALKWAY, 38f)
    }

    // ---------------------------------------------------------
    // PRE-MAPPED CAMPUS WI-FI APs & BLE BEACON ANCHORS
    // ---------------------------------------------------------
    val signalAnchors: List<CampusSignalAnchor> = listOf(
        CampusSignalAnchor(
            id = "ap_mb_g_portico",
            label = "MB Portico AP-01",
            blockId = "MB",
            floorNumber = 0,
            x = 792f,
            y = 380f,
            wifiBssids = listOf("d8:c7:c8:10:01:01", "a4:2b:8c:10:01:01"),
            wifiSsidPrefixes = listOf("MCE-CAMPUS-MB", "MCE_PORTICO", "MCE-GUEST"),
            bleMacs = listOf("AC:23:3F:A1:00:01"),
            beaconUuid = "e2c56db5-dffb-48d2-b060-d0f5a71096e0",
            roomId = "mb_g_portico"
        ),
        CampusSignalAnchor(
            id = "ap_mb_g_phys",
            label = "MB Physics Wing AP-02",
            blockId = "MB",
            floorNumber = 0,
            x = 430f,
            y = 180f,
            wifiBssids = listOf("d8:c7:c8:10:01:02"),
            wifiSsidPrefixes = listOf("MCE-PHYSICS-AP"),
            bleMacs = listOf("AC:23:3F:A1:00:02"),
            roomId = "mb_g_phys_e"
        ),
        CampusSignalAnchor(
            id = "ap_mb_1_admin",
            label = "MB 1F Principal Wing AP-03",
            blockId = "MB",
            floorNumber = 1,
            x = 550f,
            y = 525f,
            wifiBssids = listOf("d8:c7:c8:10:02:01"),
            wifiSsidPrefixes = listOf("MCE-ADMIN-1F"),
            bleMacs = listOf("AC:23:3F:A1:01:01"),
            roomId = "mb_1_vp"
        ),
        CampusSignalAnchor(
            id = "ap_mb_2_coe",
            label = "MB 2F CoE Corridor AP-04",
            blockId = "MB",
            floorNumber = 2,
            x = 430f,
            y = 180f,
            wifiBssids = listOf("d8:c7:c8:10:03:01"),
            wifiSsidPrefixes = listOf("MCE-COE-202", "MCE-MB2F"),
            bleMacs = listOf("AC:23:3F:A1:02:01"),
            roomId = "mb_2_202"
        ),
        CampusSignalAnchor(
            id = "ap_ab_g_atrium",
            label = "AB Atrium AP-05",
            blockId = "AB",
            floorNumber = 0,
            x = 115f,
            y = 348f,
            wifiBssids = listOf("d8:c7:c8:20:01:01"),
            wifiSsidPrefixes = listOf("MCE-AB-ATRIUM"),
            bleMacs = listOf("AC:23:3F:B1:00:01"),
            roomId = "ab_g_atrium"
        ),
        CampusSignalAnchor(
            id = "ap_ab_2_corridor",
            label = "AB 2F Spine AP-06",
            blockId = "AB",
            floorNumber = 2,
            x = 473f,
            y = 348f,
            wifiBssids = listOf("d8:c7:c8:20:03:01"),
            wifiSsidPrefixes = listOf("MCE-AB-2F"),
            bleMacs = listOf("AC:23:3F:B1:02:01"),
            roomId = "ab_2_204"
        )
    )

    fun searchRooms(query: String, category: RoomCategory? = null): List<CampusRoom> {
        val q = query.trim().lowercase()
        return allRooms.filter { room ->
            val matchesCat = category == null || room.category == category
            val matchesQuery = q.isEmpty() ||
                room.code.lowercase().contains(q) ||
                room.name.lowercase().contains(q) ||
                room.blockId.lowercase().contains(q) ||
                room.wing.lowercase().contains(q) ||
                room.description.lowercase().contains(q) ||
                room.keywords.any { it.lowercase().contains(q) }
            matchesCat && matchesQuery
        }.sortedByDescending { room ->
            if (q.isEmpty()) 0
            else when {
                room.code.lowercase() == q -> 3
                room.code.lowercase().startsWith(q) -> 2
                room.name.lowercase().startsWith(q) -> 1
                else -> 0
            }
        }
    }
}
