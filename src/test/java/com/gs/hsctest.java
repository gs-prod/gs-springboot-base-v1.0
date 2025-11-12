
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import com.alibaba.dubbo.common.utils.LogUtil;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormRow;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigDecimal;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
public void createPremium() throws SQLException {
    savePremiums();
}

public void  savePremiums() throws SQLException {
    Logger logger = Logger.getLogger("MyCustomLogger1.2");
    logger.info("savePremiums?v=123123.");
    Connection con = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        // 查询group 对像 获取 保费类型和公式，货币种类。
        con = getConnection();
        con.setAutoCommit(false);
        String userName = "#currentUser.username#";
        //String userName = "cat";
        // 查询临时表customers 导入的数据
        List listCustomers = findTemCustomers(con, stmt, rs, userName);

        List familyObject = new ArrayList();
        String groupId = "";
        String premiumtype = "";
        String premium = "";
        String c_currency = "";
        String family_id = "";
        String familyNo = "";
        String c_member_id = null;
        int index = 0;
        int spouse = 0;
        int children = 0;
        String employflag = null;
        String group_startTime = "";
        String group_expiry_time = "";
        String code = "";
        Map familyCount = new HashMap();
        for (Object customerMap : listCustomers) {
            String currentgroupId = (String) ((Map) customerMap).get("c_groupId");
            Map paramCustomerMap = checkIsHaveCustomer((Map) customerMap, con, stmt, rs);
            String c_customer_id = (String) paramCustomerMap.get("c_Customer_id");
            String familyId = (String) paramCustomerMap.get("c_familyId");
            if (c_customer_id != null && c_customer_id != "") {
                if (checkIsHaveMember(c_customer_id, currentgroupId, con, stmt, rs)) {
                    logger.info("c_customer_id:" + c_customer_id + ".line61");
                    logger.info("currentgroupId:" + currentgroupId + ".line62");
                    logger.info("Forename:" + (String) ((Map) customerMap).get("c_Forename") + ".line63");
                    continue;
                }
            }

            if (!groupId.equals(currentgroupId)) {
                groupId = currentgroupId.trim();
                // 查询根据Group 查询出Account的CODE
                code = findCodeBygroupId(con, stmt, rs, groupId);

                Map groupObject = findGroupBygroupId(con, stmt, rs, groupId);
                if (groupObject.size() == 0) {
                    return;
                }

                premiumtype = (String) groupObject.get("premium-type");
                premium = (String) groupObject.get("c_premiumid");
                c_currency = (String) groupObject.get("c_currency");
                group_startTime = (String) groupObject.get("c_startTime");
                group_expiry_time = (String) groupObject.get("c_expiry_time");
            }

            // 是否新入保否则退出
            String c_CensusType = (String) ((Map) customerMap).get("c_CensusType");
            String memberType = (String) ((Map) customerMap).get("c_MemberType");
            logger.info("c_CensusType(line:89):" + c_CensusType);
            logger.info("c_MemberType(line:90):" + memberType);
            if ("new".equalsIgnoreCase(c_CensusType)) {
                logger.info("in new.");
                ((Map) customerMap).put("c_groupid", groupId);
                if ("Age-Band".equalsIgnoreCase(premiumtype)) {
                    logger.info("in Age-Band.(line:89)");
                    String[] arrages = premium.split("@");
                    List listAgesMoney = new ArrayList();
                    for (int i = 0; i < arrages.length; i++) {
                        listAgesMoney.add(arrages[i]);
                    }
                    // 按照Age-Band 方式来计算保费
                    Map premiumMap = new HashMap();
                    setPremiumMap(premiumMap, (Map) customerMap);
                    countPremiumInAge(c_CensusType, group_startTime, (Map) customerMap, premiumMap, listAgesMoney,
                            c_currency);
                    logger.info("in Age-Band.(line:100)");
                    premiumMap.put("groupId", groupId);
                    premiumMap.put("c_premiumtype", premiumtype);
                    ((Map) customerMap).put("c_groupid", groupId);
                    logger.info("befor employee pattern Age-Band.(line:103)");
                    if ("Employee".equals(memberType)) {
                        logger.info("in Employee.(line:105)");
                        spouse = 0;
                        children = 0;
                        int seqnumber = sumMembers(con, stmt, rs, groupId);
                        familyNo = getSeq(seqnumber);
                        employflag = "EE";
                        // c_member_id
                        // 规则：CN（国家）-SSIS-Suzhou（accountID）-200101(前两位是年份；中间两位是policyID序列号；后两位是groupID序列号）-002（family序列号）-EE（员工身份）
                        c_member_id = code + "-" + getYearHalf(groupId) + groupId.split("-")[groupId.split("-").length - 2]
                                + groupId.split("-")[groupId.split("-").length - 1].substring(2, 4) + "-"
                                + familyNo;

                        if (c_customer_id == null || c_customer_id == "") {
                            family_id = "familyid" + System.currentTimeMillis();
                        } else {
                            family_id = familyId;
                        }

                        String employee_member_id = c_member_id + "-" + employflag;
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", employee_member_id);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }

                        loadMemberData((Map) customerMap, con, stmt);
                    } else if ("Spouse".equalsIgnoreCase(memberType)) {
                        logger.info("in Spouse.");
                        spouse++;
                        employflag = "SP";

                        String spouse_member_id = c_member_id + "-" + employflag;
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", spouse_member_id);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }
                        loadMemberData((Map) customerMap, con, stmt);
                    } else if ("Children".equalsIgnoreCase(memberType)) {
                        logger.info("in Children.");
                        children++;
                        employflag = "C" + children;
                        String children_member_id = c_member_id + "-" + employflag;
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", children_member_id);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }
                        loadMemberData((Map) customerMap, con, stmt);
                    }
                    logger.info("in Land the premium.(line:185)");
                    // 将保费落地
                    premiumMap.put("c_family_id", family_id);
                    premiumMap.put("c_StartDate", group_startTime);
                    premiumMap.put("c_ExpiryDate", group_expiry_time);
                    premiumMap.put("dateCreated", getCurrentTime());
                    premiumMap.put("createdBy", userName);
                    premiumMap.put("c_Customer_id", ((Map) customerMap).get("c_Customer_id"));
                    premiumMap.put("c_member_id", ((Map) customerMap).get("c_member_id"));
                    savePremiumData(premiumMap, con);

                } else {// 当计算类型为Family-Rate 类型时
                    logger.info("in Family-Rate.");
                    if ("Employee".equalsIgnoreCase(memberType)) {
                        if (familyCount.size() > 0) {
                            String premiumMoney = countPremiumInFamily(premium, familyCount);
                            // 如果员工是加保进来的
                            Map EmployeeCustomer = (Map) familyCount.get("customer");
                            String employeeCensusType = (String) EmployeeCustomer.get("c_CensusType");
                            String age = "";
                            Map premiumMap = new HashMap();
                            if ("add".equalsIgnoreCase(employeeCensusType)) {
                                premiumMoney = duePremiumMoney(group_startTime, group_expiry_time,
                                        String.valueOf(premiumMoney), (String) EmployeeCustomer.get("c_StartDate"));
                                age = countPremiumAge(employeeCensusType,
                                        (String) EmployeeCustomer.get("c_StartDate"),
                                        (String) EmployeeCustomer.get("c_DateofBirth"));
                                premiumMap.put("c_StartDate", (String) EmployeeCustomer.get("c_StartDate"));
                                premiumMap.put("c_ExpiryDate", (String) EmployeeCustomer.get("c_ExpiryDate"));
                            } else {
                                // 入保年龄
                                age = countPremiumAge(employeeCensusType, group_startTime,
                                        (String) EmployeeCustomer.get("c_DateofBirth"));
                                premiumMap.put("c_StartDate", group_startTime);
                                premiumMap.put("c_ExpiryDate", group_startTime);
                            }
                            setPremiumMap(premiumMap, (Map) familyCount.get("customer"));
                            premiumMap.put("age", age);
                            premiumMap.put("premium", premiumMoney);
                            premiumMap.put("c_currency", c_currency);
                            premiumMap.put("groupId", groupId);
                            premiumMap.put("c_premiumtype", premiumtype);
                            premiumMap.put("c_Customer_id", EmployeeCustomer.get("c_Customer_id"));
                            premiumMap.put("dateCreated", getCurrentTime());
                            premiumMap.put("createdBy", userName);
                            // 将保费落地
                            savePremiumData(premiumMap, con);
                        }
                        familyCount.clear();
                        spouse = 0;
                        children = 0;
                        // customer 数据落地
                        // 生成memberid 并落地数据到member表中
                        int seqnumber = sumMembers(con, stmt, rs, groupId);
                        familyNo = getSeq(seqnumber);
                        employflag = "EE";

                        c_member_id = code + "-" + getYearHalf(groupId) + groupId.split("-")[groupId.split("-").length - 2]
                                + groupId.split("-")[groupId.split("-").length - 1].substring(2, 4) + "-"
                                + familyNo;
                        String employee_member_id = c_member_id + "-" + employflag;
                        if (c_customer_id == null || c_customer_id == "") {
                            family_id = "familyid" + System.currentTimeMillis();
                        } else {
                            family_id = familyId;
                        }
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", employee_member_id);

                        familyCount.put("Employee", 1);
                        familyCount.put("Spouse", spouse);
                        familyCount.put("Children", children);
                        familyCount.put("customer", customerMap);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }
                        loadMemberData((Map) customerMap, con, stmt);
                        index++;
                    } else if ("Spouse".equalsIgnoreCase(memberType)) {
                        spouse++;
                        familyCount.put("Spouse", spouse);
                        employflag = "SP";
                        String spouse_member_id = c_member_id + "-" + employflag;
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", spouse_member_id);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }
                        loadMemberData((Map) customerMap, con, stmt);
                        index++;
                        addPremiumFamilyOthers((Map) customerMap, c_CensusType, group_startTime, c_currency,
                                groupId, premiumtype, userName, con);
                    } else if ("Children".equalsIgnoreCase(memberType)) {
                        children++;
                        familyCount.put("Children", children);
                        employflag = "C" + children;
                        String children_member_id = c_member_id + "-" + employflag;
                        ((Map) customerMap).put("c_family_id", family_id);
                        ((Map) customerMap).put("c_member_id", children_member_id);
                        ((Map) customerMap).put("c_StartDate", group_startTime);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("familyNo", familyNo);
                        ((Map) customerMap).put("createdBy", userName);
                        ((Map) customerMap).put("dateCreated", getCurrentTime());
                        if (c_customer_id == null || c_customer_id == "") {
                            String c_Customer_id = System.currentTimeMillis() + "-"
                                    + ((Map) customerMap).get("c_IDNumber")
                                    + ((Map) customerMap).get("c_NationalityCode");
                            ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                        } else {
                            ((Map) customerMap).put("c_Customer_id", c_customer_id);
                        }
                        loadMemberData((Map) customerMap, con, stmt);

                        index++;

                        addPremiumFamilyOthers((Map) customerMap, c_CensusType, group_startTime, c_currency,
                                groupId, premiumtype, userName, con);

                    }

                    if (listCustomers.size() == index) {
                        String premiumMoney = countPremiumInFamily(premium, familyCount);
                        Map EmployeeCustomer = (Map) familyCount.get("customer");
                        String employeeCensusType = (String) EmployeeCustomer.get("c_CensusType");
                        Map premiumMap = new HashMap();
                        setPremiumMap(premiumMap, ((Map) familyCount.get("customer")));
                        // 入保年龄
                        String age = countPremiumAge(employeeCensusType, group_startTime,
                                (String) ((Map) familyCount.get("customer")).get("c_DateofBirth"));
                        premiumMap.put("age", age);
                        premiumMap.put("premium", premiumMoney);
                        premiumMap.put("c_currency", c_currency);
                        premiumMap.put("groupId", groupId);
                        premiumMap.put("c_premiumtype", premiumtype);
                        premiumMap.put("c_StartDate", group_startTime);
                        premiumMap.put("c_ExpiryDate", group_expiry_time);
                        premiumMap.put("c_Customer_id", ((Map) customerMap).get("c_Customer_id"));
                        premiumMap.put("dateCreated", getCurrentTime());
                        premiumMap.put("createdBy", userName);
                        // 将保费落地
                        savePremiumData(premiumMap, con);
                    }
                }
                // customer 数据落地
                ((Map) customerMap).put("c_StartDate", group_startTime);
                ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);

                // 判断是否存在这个人
                // 如果存在则不用落地直接生成对应的卡信息和保费信息
                if (c_customer_id == null || c_customer_id == "") {
                    loadDueCustomer((Map) customerMap, con, stmt);
                }

            } else if ("add".equalsIgnoreCase(c_CensusType)) {
                logger.info("add c_CensusType");
                // 加保业务
                ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);

                if (StringUtils.isEmpty(((String) ((Map) customerMap).get("familyNo")))) {
                    /**
                     * //整个家庭加保的时
                     */
                    ((Map) customerMap).put("c_groupid", groupId);
                    if ("Age-Band".equalsIgnoreCase(premiumtype)) {
                        String[] arrages = premium.split("@");
                        List listAgesMoney = new ArrayList();
                        for (int i = 0; i < arrages.length; i++) {
                            listAgesMoney.add(arrages[i]);
                        }
                        // 按照Age-Band 方式来计算保费
                        Map premiumMap = new HashMap();
                        setPremiumMap(premiumMap, (Map) customerMap);
                        countPremiumInAge(c_CensusType, (String) ((Map) customerMap).get("c_StartDate"),
                                (Map) customerMap, premiumMap, listAgesMoney, c_currency);
                        // 如果是加保的员工需要计算一下实际的保费了
                        String premiumMoney = ((BigDecimal) premiumMap.get("premium")).toString();
                        String premiumtrueMoney = duePremiumMoney(group_startTime, group_expiry_time, premiumMoney,
                                (String) ((Map) customerMap).get("c_StartDate"));

                        premiumMap.put("premium", premiumtrueMoney);
                        premiumMap.put("groupId", groupId);
                        premiumMap.put("c_premiumtype", premiumtype);
                        ((Map) customerMap).put("c_ExpiryDate", group_expiry_time);
                        ((Map) customerMap).put("c_groupid", groupId);
                        if ("Employee".equalsIgnoreCase(memberType)) {
                            spouse = 0;
                            children = 0;
                            int seqnumber = sumMembers(con, stmt, rs, groupId);
                            familyNo = getSeq(seqnumber);
                            employflag = "EE";
                            c_member_id = code + "-" + getYearHalf(groupId)
                                    + groupId.split("-")[groupId.split("-").length - 2]
                                    + groupId.split("-")[groupId.split("-").length - 1].substring(2, 4) + "-"
                                    + familyNo;
                            if (c_customer_id == null || c_customer_id == "") {
                                family_id = "familyid" + System.currentTimeMillis();
                            } else {
                                family_id = familyId;
                            }
                            String employee_member_id = c_member_id + "-" + employflag;
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", employee_member_id);
                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);
                        } else if ("Spouse".equalsIgnoreCase(memberType)) {
                            spouse++;
                            employflag = "SP";
                            String spouse_member_id = c_member_id + "-" + employflag;
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", spouse_member_id);
                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);
                        } else if ("Children".equalsIgnoreCase(memberType)) {
                            children++;
                            employflag = "C" + children;
                            String children_member_id = c_member_id + "-" + employflag;
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", children_member_id);
                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);
                        }
                        // 将保费落地
                        if (c_customer_id != null && c_customer_id != "") {
                            family_id = familyId;
                        }
                        premiumMap.put("c_family_id", family_id);
                        premiumMap.put("c_Customer_id", ((Map) customerMap).get("c_Customer_id"));
                        premiumMap.put("dateCreated", getCurrentTime());
                        premiumMap.put("createdBy", userName);
                        premiumMap.put("c_member_id", ((Map) customerMap).get("c_member_id"));
                        savePremiumData(premiumMap, con);

                    } else {// 加保当计算类型为Family-Rate 类型时
                        if ("Employee".equalsIgnoreCase(memberType)) {
                            if (familyCount.size() > 0) {
                                String premiumMoney = countPremiumInFamily(premium, familyCount);
                                // 如果员工是加保进来的
                                String age = "";
                                Map EmployeeCustomer = (Map) familyCount.get("customer");
                                String employeeCensusType = (String) EmployeeCustomer.get("c_CensusType");
                                Map premiumMap = new HashMap();
                                if ("add".equalsIgnoreCase(employeeCensusType)) {
                                    premiumMoney = duePremiumMoney(group_startTime, group_expiry_time,
                                            String.valueOf(premiumMoney),
                                            (String) EmployeeCustomer.get("c_StartDate"));
                                    // 入保年龄
                                    age = countPremiumAge(employeeCensusType,
                                            (String) EmployeeCustomer.get("c_StartDate"),
                                            (String) EmployeeCustomer.get("c_DateofBirth"));
                                    premiumMap.put("c_StartDate", (String) EmployeeCustomer.get("c_StartDate"));
                                    premiumMap.put("c_ExpiryDate", (String) EmployeeCustomer.get("c_ExpiryDate"));
                                } else {
                                    age = countPremiumAge(employeeCensusType, group_startTime,
                                            (String) EmployeeCustomer.get("c_DateofBirth"));
                                    premiumMap.put("c_StartDate", group_startTime);
                                    premiumMap.put("c_ExpiryDate", group_expiry_time);
                                }

                                setPremiumMap(premiumMap, (Map) familyCount.get("customer"));
                                premiumMap.put("age", age);
                                premiumMap.put("premium", premiumMoney);
                                premiumMap.put("c_currency", c_currency);
                                premiumMap.put("groupId", groupId);
                                premiumMap.put("c_premiumtype", premiumtype);
                                premiumMap.put("c_Customer_id", EmployeeCustomer.get("c_Customer_id"));
                                premiumMap.put("dateCreated", getCurrentTime());
                                premiumMap.put("createdBy", userName);
                                // 将保费落地
                                savePremiumData(premiumMap, con);
                            }
                            familyCount.clear();
                            spouse = 0;
                            children = 0;
                            // customer 数据落地
                            // 生成memberid 并落地数据到member表中
                            int seqnumber = sumMembers(con, stmt, rs, groupId);
                            familyNo = getSeq(seqnumber);
                            employflag = "EE";
                            c_member_id = code + "-" + getYearHalf(groupId)
                                    + groupId.split("-")[groupId.split("-").length - 2]
                                    + groupId.split("-")[groupId.split("-").length - 1].substring(2, 4) + "-"
                                    + familyNo;
                            String employee_member_id = c_member_id + "-" + employflag;
                            if (c_customer_id == null || c_customer_id == "") {
                                family_id = "familyid" + System.currentTimeMillis();
                            } else {
                                family_id = familyId;
                            }
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", employee_member_id);

                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            familyCount.put("Employee", 1);
                            familyCount.put("Spouse", spouse);
                            familyCount.put("Children", children);
                            familyCount.put("customer", customerMap);
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);
                            index++;
                        } else if ("Spouse".equalsIgnoreCase(memberType)) {
                            spouse++;
                            familyCount.put("Spouse", spouse);
                            employflag = "SP";
                            String spouse_member_id = c_member_id + "-" + employflag;
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", spouse_member_id);
                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);
                            index++;
                            addPremiumFamilyOthers((Map) customerMap, c_CensusType, group_startTime, c_currency,
                                    groupId, premiumtype, userName, con);

                        } else if ("Children".equalsIgnoreCase(memberType)) {
                            children++;
                            familyCount.put("Children", children);
                            employflag = "C" + children;
                            String children_member_id = c_member_id + "-" + employflag;
                            ((Map) customerMap).put("c_family_id", family_id);
                            ((Map) customerMap).put("c_member_id", children_member_id);
                            if (c_customer_id == null || c_customer_id == "") {
                                String c_Customer_id = System.currentTimeMillis() + "-"
                                        + ((Map) customerMap).get("c_IDNumber")
                                        + ((Map) customerMap).get("c_NationalityCode");
                                ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                            } else {
                                ((Map) customerMap).put("c_Customer_id", c_customer_id);
                            }
                            ((Map) customerMap).put("familyNo", familyNo);
                            ((Map) customerMap).put("createdBy", userName);
                            ((Map) customerMap).put("dateCreated", getCurrentTime());
                            loadMemberData((Map) customerMap, con, stmt);

                            index++;
                            addPremiumFamilyOthers((Map) customerMap, c_CensusType, group_startTime, c_currency,
                                    groupId, premiumtype, userName, con);

                        }

                        if (listCustomers.size() == index) {
                            String premiumMoney = countPremiumInFamily(premium, familyCount);
                            Map EmployeeCustomer = (Map) familyCount.get("customer");
                            String employeeCensusType = (String) EmployeeCustomer.get("c_CensusType");
                            premiumMoney = duePremiumMoney(group_startTime, group_expiry_time,
                                    String.valueOf(premiumMoney), (String) EmployeeCustomer.get("c_StartDate"));
                            Map premiumMap = new HashMap();
                            setPremiumMap(premiumMap, ((Map) familyCount.get("customer")));
                            // 入保年龄
                            String age = countPremiumAge(employeeCensusType,
                                    (String) EmployeeCustomer.get("c_StartDate"),
                                    (String) ((Map) familyCount.get("customer")).get("c_DateofBirth"));
                            premiumMap.put("age", age);
                            premiumMap.put("premium", premiumMoney);
                            premiumMap.put("c_currency", c_currency);
                            premiumMap.put("groupId", groupId);
                            premiumMap.put("c_premiumtype", premiumtype);
                            premiumMap.put("c_Customer_id", EmployeeCustomer.get("c_Customer_id"));
                            premiumMap.put("dateCreated", getCurrentTime());
                            premiumMap.put("createdBy", userName);
                            // 将保费落地
                            savePremiumData(premiumMap, con);
                        }
                    }
                    // customer 数据落地
                    if (c_customer_id == null || c_customer_id == "") {
                        loadDueCustomer((Map) customerMap, con, stmt);
                    }

                    continue;
                }

                // 家庭成员加保
                Map paramMap = new HashMap();
                paramMap.put("premiumType", premiumtype);// 保费公式类型
                paramMap.put("MemberType", memberType);// 家庭类型
                paramMap.put("c_premiumid", premium);// 保费公式
                // 通过familyNo查询到FaiilyId
                Map map = findFamilyIdByFamilyNo((String) ((Map) customerMap).get("familyNo"), groupId, con, stmt,
                        rs);
                paramMap.put("familyId", map.get("c_family_id"));// 家庭
                paramMap.put("c_member_id", map.get("c_member_id"));// 家庭
                paramMap.put("c_startTime", group_startTime);
                paramMap.put("c_expiry_time", group_expiry_time);
                paramMap.put("userName", userName);
                Map premiumMap = new HashMap();
                premiumMap.put("c_currency", c_currency);
                premiumMap.put("groupId", groupId);
                ((Map) customerMap).put("c_groupid", groupId);
                ((Map) customerMap).put("c_family_id", map.get("c_family_id"));
                if ("Age-Band".equalsIgnoreCase(premiumtype)) {
                    setPremiumMap(premiumMap, (Map) customerMap);
                    premiumMap.put("c_Customer_id", ((Map) customerMap).get("c_Customer_id"));
                } else {
                    // 获取该家庭的员工信息将保费记录在他的头上
                    premiumMap.put("c_StartDate", (String) ((Map) customerMap).get("c_StartDate"));
                    premiumMap.put("c_ExpiryDate", group_expiry_time);
                    premiumMap.put("c_family_id", (String) map.get("c_family_id"));
                    setPremiumByFamilyId(premiumMap, groupId, map, con, stmt, rs);
                }
                ((Map) customerMap).put("createdBy", userName);
                ((Map) customerMap).put("dateCreated", getCurrentTime());
                if (c_customer_id == null || c_customer_id == "") {
                    String c_Customer_id = System.currentTimeMillis() + "-" + ((Map) customerMap).get("c_IDNumber")
                            + ((Map) customerMap).get("c_NationalityCode");
                    ((Map) customerMap).put("c_Customer_id", c_Customer_id);
                } else {
                    ((Map) customerMap).put("c_Customer_id", c_customer_id);
                }

                addInsurance(c_CensusType, paramMap, premiumMap, (Map) customerMap, con, stmt, rs);
                // customer 数据落地

                if (c_customer_id == null || c_customer_id == "") {
                    loadDueCustomer((Map) customerMap, con, stmt);
                }
            }

        }
        logger.info("beforDelete.(line:672)");
        // 删除临时表的customers
        deleteCustomer(userName, con, stmt);
        con.commit();

    } catch (Exception e) {
        e.printStackTrace();
        con.rollback();
    }finally {
        CloseCon(con, stmt, rs);
    }

}

// 生成家庭保费时，除了EE 其他家庭也要生成一条保费为0的记录。
public static void addPremiumFamilyOthers(Map customerMap, String c_CensusType, String group_startTime,
                                          String c_currency, String groupId, String premiumtype, String userName, Connection con) {
    // 将保费落地
    Map premiumMap = new HashMap();
    // 入保年龄
    String age = "";
    if ("add".equalsIgnoreCase(c_CensusType)) {
        age = countPremiumAge(c_CensusType, (String) ((Map) customerMap).get("c_StartDate"),
                (String) ((Map) customerMap).get("c_DateofBirth"));
        premiumMap.put("c_StartDate", (String) ((Map) customerMap).get("c_StartDate"));
        premiumMap.put("c_ExpiryDate", (String) ((Map) customerMap).get("c_ExpiryDate"));
    } else {
        // 入保年龄
        age = countPremiumAge(c_CensusType, group_startTime, (String) ((Map) customerMap).get("c_DateofBirth"));
        premiumMap.put("c_StartDate", group_startTime);
        premiumMap.put("c_ExpiryDate", group_startTime);
    }
    setPremiumMap(premiumMap, ((Map) customerMap));
    premiumMap.put("age", age);
    premiumMap.put("premium", "0.00");
    premiumMap.put("c_currency", c_currency);
    premiumMap.put("groupId", groupId);
    premiumMap.put("c_premiumtype", premiumtype);
    premiumMap.put("c_Customer_id", ((Map) customerMap).get("c_Customer_id"));
    premiumMap.put("dateCreated", getCurrentTime());
    premiumMap.put("createdBy", userName);
    savePremiumData(premiumMap, con);

}

// 生成卡信息，保费信息，落地customers
public static void commonCreatCardAndPrimium(Object customerMap, String groupId, String premiumtype, String premium,
                                             String memberType, Connection con, PreparedStatement stmt, ResultSet rs, Map familyCount, String c_currency,
                                             List listCustomers, String startDate, String expiryDate, String c_CensusType

) {

}

// 将数据落地
public static void loadPremiumAndMemberAndDeleCus(Map premiumMap, Map customerMap, Connection con,
                                                  PreparedStatement stmt, ResultSet rs) {
    // 将保费落地
    savePremiumData(premiumMap, con);
    ((Map) customerMap).put("c_groupid", premiumMap.get("groupId"));
    ((Map) customerMap).put("c_account_id", premiumMap.get("accountId"));
    // customer 数据落地
    loadDueCustomer((Map) customerMap, con, stmt);

}

// 通过年龄段来计算保费
public static void countPremiumInAge(String c_CensusType, String groupStartTime, Map customerMap, Map premiumMap,
                                     List listAgesMoney, String c_currency) {
    Logger logger = Logger.getLogger("MyCustomLogger");
    logger.info("in countPremiumInAge.(line:742)");
    // 入保年龄
    String ageband = countPremiumAge(c_CensusType, groupStartTime,
            (String) ((Map) customerMap).get("c_DateofBirth"));
    logger.info("in countPremiumInAge.(line:746)");
    premiumMap.put("age", ageband);
    // 根据group premiu 计算保费
    String premiumMoney = (countPremiumMoney(listAgesMoney, ageband)) == "" ? "0"
            : (countPremiumMoney(listAgesMoney, ageband));
    logger.info("in countPremiumInAge.(line:751)");
    logger.info("in countPremiumInAge premiumMoney:" + premiumMoney + " .(line:752)");
    BigDecimal bigpremiumMoney = new BigDecimal(premiumMoney.replaceAll(",", "")).setScale(2, BigDecimal.ROUND_UP);
    logger.info("in countPremiumInAge bigpremiumMoney:" + bigpremiumMoney + " .(line:760)");
    premiumMap.put("premium", bigpremiumMoney);
    premiumMap.put("c_currency", c_currency);
    logger.info("end countPremiumInAge.(line:763)");

}

public static void setPremiumMap(Map premiumMap, Map customerMap) {
    Logger logger = Logger.getLogger("MyCustomLogger");
    logger.info("in setPremiumMap.(line:757)");
    premiumMap.put("c_NationalityCode", (String) ((Map) customerMap).get("c_NationalityCode"));
    premiumMap.put("c_LocationCode", (String) ((Map) customerMap).get("c_LocationCode"));
    premiumMap.put("c_Title", (String) ((Map) customerMap).get("c_Title"));
    premiumMap.put("c_Nationality", (String) ((Map) customerMap).get("c_Nationality"));
    premiumMap.put("c_EmailAddress", (String) ((Map) customerMap).get("c_EmailAddress"));
    premiumMap.put("c_StartDate", (String) ((Map) customerMap).get("c_StartDate"));
    premiumMap.put("c_MemberType", (String) ((Map) customerMap).get("c_MemberType"));
    premiumMap.put("c_CensusType", (String) ((Map) customerMap).get("c_CensusType"));
    premiumMap.put("c_Forename", (String) ((Map) customerMap).get("c_Forename"));
    premiumMap.put("c_InitialsOptional", (String) ((Map) customerMap).get("c_InitialsOptional"));
    premiumMap.put("c_DateofBirth", (String) ((Map) customerMap).get("c_DateofBirth"));
    premiumMap.put("c_Surname", (String) ((Map) customerMap).get("c_Surname"));
    premiumMap.put("c_IDNumber", (String) ((Map) customerMap).get("c_IDNumber"));
    premiumMap.put("c_Location", (String) ((Map) customerMap).get("c_Location"));
    premiumMap.put("c_GenderMaleFemale", (String) ((Map) customerMap).get("c_GenderMaleFemale"));
    premiumMap.put("c_family_id", (String) ((Map) customerMap).get("c_family_id"));
    premiumMap.put("c_ExpiryDate", (String) ((Map) customerMap).get("c_ExpiryDate"));
    premiumMap.put("c_member_id", (String) ((Map) customerMap).get("c_member_id"));
    logger.info("end setPremiumMap.(line:776)");
}

// 根据groupId 查询group数据
public static Map findGroupBygroupId(Connection con, PreparedStatement stmt, ResultSet rs, String groupId) {
    String sql = "select * from  app_fd_policygroup_table where c_group_id= ? and c_reviewstatus='End'";
    List listAgesMoney = null;
    Map groupObject = new HashMap();
    try {
        stmt = con.prepareStatement(sql);
        stmt.setString(1, groupId);
        // 执行sql语句
        rs = stmt.executeQuery();
        String premium = "";
        while (rs.next()) {
            groupObject.put("c_premiumid", (String) rs.getString("c_premiumid"));
            groupObject.put("c_currency", (String) rs.getString("c_currency"));
            groupObject.put("premium-type", (String) rs.getString("c_premiumtype"));
            groupObject.put("c_startTime", (String) rs.getString("c_EffectiveDate"));
            groupObject.put("c_expiry_time", (String) rs.getString("c_expiry_time"));

        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return groupObject;

}

// 根据groupId 查询 accountCode
public static String findCodeBygroupId(Connection con, PreparedStatement stmt, ResultSet rs, String groupId) {
    String sql = "SELECT  a.c_code  FROM app_fd_account_table  a"
            + " LEFT JOIN app_fd_policy_table p ON  a.c_account_id=p.c_account_id "
            + "LEFT JOIN app_fd_policygroup_table g ON p.c_policyId=g.c_policy_id  " + "WHERE g.c_group_id=?";
    List listAgesMoney = null;
    String c_code = "";
    try {
        stmt = con.prepareStatement(sql);
        stmt.setString(1, groupId);
        // 执行sql语句
        rs = stmt.executeQuery();
        String premium = "";
        while (rs.next()) {
            c_code = (String) rs.getString("c_code");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return c_code;
}

/**
 * up 查询出所有临时表中的customer记录
 *
 * @return
 */
public static List findTemCustomers(Connection con, PreparedStatement stmt, ResultSet rs, String userName) {
    String sqlcustomers = "select * from  app_fd_customerimport_table  where createdBy=? ORDER BY c_seq ";
    List listCustomerMap = new ArrayList();
    try {
        stmt = con.prepareStatement(sqlcustomers);
        stmt.setString(1, userName);
        rs = stmt.executeQuery();

        // 保费数据记录
        while (rs.next()) {
            Map customerMap = new HashMap();
            String id = rs.getString("id");
            String c_groupId = rs.getString("c_groupId");
            String c_ExpiryDate = rs.getString("c_ExpiryDate");
            String c_GenderMaleFemale = rs.getString("c_GenderMaleFemale");
            String c_NationalityCode = rs.getString("c_NationalityCode");
            String c_LocationCode = rs.getString("c_LocationCode");
            String c_Title = rs.getString("c_Title");
            String c_Nationality = rs.getString("c_Nationality");
            String c_EmailAddress = rs.getString("c_EmailAddress");
            String c_StartDate = rs.getString("c_StartDate");
            String c_MemberType = rs.getString("c_MemberType");
            String c_CensusType = rs.getString("c_CensusType");
            String c_Forename = rs.getString("c_Forename");
            String c_InitialsOptional = rs.getString("c_InitialsOptional");
            String c_DateofBirth = rs.getString("c_DateofBirth");
            String c_Surname = rs.getString("c_Surname");
            String c_IDNumber = rs.getString("c_IDNumber");
            String c_Location = rs.getString("c_Location");
            String familyNo = rs.getString("c_familyNo");
            String c_Gender = rs.getString("c_GenderMaleFemale");
            String c_customernumber = rs.getString("c_customernumber");
            String c_IDType = rs.getString("c_IDType");
            String c_Startingdateofcertificate = rs.getString("c_Startingdateofcertificate");
            String c_certificateterminationdate = rs.getString("c_certificateterminationdate");

            customerMap.put("id", id);

            customerMap.put("c_groupId", c_groupId);
            customerMap.put("c_ExpiryDate", c_ExpiryDate);
            customerMap.put("c_GenderMaleFemale", c_GenderMaleFemale);
            customerMap.put("c_NationalityCode", c_NationalityCode);
            customerMap.put("c_LocationCode", c_LocationCode);
            customerMap.put("c_Title", c_Title);
            customerMap.put("c_Nationality", c_Nationality);
            customerMap.put("c_EmailAddress", c_EmailAddress);
            customerMap.put("c_StartDate", c_StartDate);
            customerMap.put("c_MemberType", c_MemberType);
            customerMap.put("c_CensusType", c_CensusType);
            customerMap.put("c_Forename", c_Forename);
            customerMap.put("c_InitialsOptional", c_InitialsOptional);
            customerMap.put("c_DateofBirth", c_DateofBirth);
            customerMap.put("c_Surname", c_Surname);
            customerMap.put("c_IDNumber", c_IDNumber);
            customerMap.put("c_Location", c_Location);
            customerMap.put("familyNo", familyNo);
            customerMap.put("c_Gender", c_Gender);
            customerMap.put("c_customernumber", c_customernumber);

            customerMap.put("c_IDType", c_IDType);
            customerMap.put("c_Startingdateofcertificate", c_Startingdateofcertificate);
            customerMap.put("c_certificateterminationdate", c_certificateterminationdate);
            listCustomerMap.add(customerMap);

        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return listCustomerMap;

}

public static int sumMembers(Connection con, PreparedStatement stmt, ResultSet rs, String groupId) {
    String sqlSumMember = "select c_maxSeq from  app_fd_familyseq_table Where c_groupId=?";
    String sqlInsert = "INSERT INTO app_fd_familyseq_table(id,c_groupId,c_maxSeq)VALUES(?,?,?)";
    String sqlUpdate = "UPDATE  app_fd_familyseq_table SET c_maxSeq=? WHERE c_groupId=?";
    int number = 0;
    String isHave = "";
    // 用于记录家庭编号最大值
    try {
        stmt = con.prepareStatement(sqlSumMember);
        stmt.setString(1, groupId);
        rs = stmt.executeQuery();
        while (rs.next()) {
            isHave = "have";
            number = Integer.parseInt(rs.getString(1));
        }
        if ("have".equals(isHave)) {
            stmt = con.prepareStatement(sqlUpdate);
            stmt.setString(1, String.valueOf(++number));
            stmt.setString(2, groupId);
            int row = stmt.executeUpdate();
            return number;
        } else {
            stmt = con.prepareStatement(sqlInsert);
            stmt.setString(1, String.valueOf(System.currentTimeMillis()));
            stmt.setString(2, groupId);
            stmt.setString(3, String.valueOf(++number));
            int row = stmt.executeUpdate();
            return number;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return number;
}

public static String getSeq(int number) {

    String seqnumber = String.valueOf(number);
    for (int i = 0; i < 3; i++) {
        seqnumber = seqnumber.length() < 3 ? "0" + seqnumber : seqnumber;
    }
    return seqnumber;
}

/**
 * 将导入的数据落地到正式表里
 */
public static void loadDueCustomer(Map customerMap, Connection con, PreparedStatement stmt) {
    String insertCustomerSql = "insert into app_fd_duecustomer_table" + "(id,c_IDNumber,c_Location,c_Surname,"
            + "c_DateofBirth,c_InitialsOptional,c_Forename,c_CensusType,"
            + "c_MemberType,c_StartDate,c_EmailAddress,c_Nationality,"
            + "c_Gender,c_Title,c_LocationCode,c_NationalityCode,c_ExpiryDate,c_familyId,c_Customer_id,c_customernumber,"
            + " c_IDType,c_Startingdateofcertificate,c_certificateterminationdate,dateCreated,createdBy)" + "values"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    try {
        stmt = con.prepareStatement(insertCustomerSql);
        stmt.setString(1, "CUSTOMER" + String.valueOf(System.currentTimeMillis())
                + String.valueOf(((int) ((Math.random() * 9 + 1) * 100000))));
        stmt.setString(2, (String) ((Map) customerMap).get("c_IDNumber") == null ? ""
                : (String) ((Map) customerMap).get("c_IDNumber"));
        stmt.setString(3, (String) ((Map) customerMap).get("c_Location") == null ? ""
                : (String) ((Map) customerMap).get("c_Location"));
        stmt.setString(4, (String) ((Map) customerMap).get("c_Surname") == null ? ""
                : (String) ((Map) customerMap).get("c_Surname"));
        stmt.setString(5, (String) ((Map) customerMap).get("c_DateofBirth") == null ? ""
                : (String) ((Map) customerMap).get("c_DateofBirth"));
        stmt.setString(6, (String) ((Map) customerMap).get("c_InitialsOptional") == null ? ""
                : (String) ((Map) customerMap).get("c_InitialsOptional"));
        stmt.setString(7, (String) ((Map) customerMap).get("c_Forename") == null ? ""
                : (String) ((Map) customerMap).get("c_Forename"));
        stmt.setString(8, (String) ((Map) customerMap).get("c_CensusType") == null ? ""
                : (String) ((Map) customerMap).get("c_CensusType"));
        stmt.setString(9, (String) ((Map) customerMap).get("c_MemberType") == null ? ""
                : (String) ((Map) customerMap).get("c_MemberType"));
        stmt.setString(10, (String) ((Map) customerMap).get("c_StartDate") == null ? ""
                : (String) ((Map) customerMap).get("c_StartDate"));
        stmt.setString(11, (String) ((Map) customerMap).get("c_EmailAddress") == null ? ""
                : (String) ((Map) customerMap).get("c_EmailAddress"));
        stmt.setString(12, (String) ((Map) customerMap).get("c_Nationality") == null ? ""
                : (String) ((Map) customerMap).get("c_Nationality"));
        stmt.setString(13, (String) ((Map) customerMap).get("c_Gender") == null ? ""
                : (String) ((Map) customerMap).get("c_Gender"));
        stmt.setString(14, (String) ((Map) customerMap).get("c_Title") == null ? ""
                : (String) ((Map) customerMap).get("c_Title"));
        stmt.setString(15, (String) ((Map) customerMap).get("c_LocationCode") == null ? ""
                : (String) ((Map) customerMap).get("c_LocationCode"));
        stmt.setString(16, (String) ((Map) customerMap).get("c_NationalityCode") == null ? ""
                : (String) ((Map) customerMap).get("c_NationalityCode"));
        stmt.setString(17, (String) ((Map) customerMap).get("c_ExpiryDate") == null ? ""
                : (String) ((Map) customerMap).get("c_ExpiryDate"));
        stmt.setString(18, (String) ((Map) customerMap).get("c_family_id") == null ? ""
                : (String) ((Map) customerMap).get("c_family_id"));
        stmt.setString(19, (String) ((Map) customerMap).get("c_Customer_id") == null ? ""
                : (String) ((Map) customerMap).get("c_Customer_id"));
        stmt.setString(20, (String) ((Map) customerMap).get("c_customernumber") == null ? ""
                : (String) ((Map) customerMap).get("c_customernumber"));
        stmt.setString(21, (String) ((Map) customerMap).get("c_IDType") == null ? ""
                : (String) ((Map) customerMap).get("c_IDType"));
        stmt.setString(22, (String) ((Map) customerMap).get("c_Startingdateofcertificate") == null ? ""
                : (String) ((Map) customerMap).get("c_Startingdateofcertificate"));
        stmt.setString(23, (String) ((Map) customerMap).get("c_certificateterminationdate") == null ? ""
                : (String) ((Map) customerMap).get("c_certificateterminationdate"));

        stmt.setString(24, (String) ((Map) customerMap).get("dateCreated") == null ? ""
                : (String) ((Map) customerMap).get("dateCreated"));
        stmt.setString(25, (String) ((Map) customerMap).get("createdBy") == null ? ""
                : (String) ((Map) customerMap).get("createdBy"));
        int row = stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }

}

/**
 * 判断customer 是否组存在
 *
 * @param id
 * @param con
 * @param stmt
 * @param rs
 * @return
 */
public static Map checkIsHaveCustomer(Map customer, Connection con, PreparedStatement stmt, ResultSet rs) {
    String sql = "SELECT c_Customer_id,c_familyId from app_fd_duecustomer_table p " + "where "
            + "p.c_Surname=? and p.c_Forename=? and p.c_Gender=? "
            + "and p.c_IDNumber=? and p.c_DateofBirth=?  and p.c_NationalityCode=?";
    Map map = new HashMap();
    String c_Customer_id = "";
    try {
        stmt = con.prepareStatement(sql);
        stmt.setString(1, (String) customer.get("c_Surname"));
        stmt.setString(2, (String) customer.get("c_Forename"));
        stmt.setString(3, (String) customer.get("c_Gender"));
        stmt.setString(4, (String) customer.get("c_IDNumber"));
        stmt.setString(5, (String) customer.get("c_DateofBirth"));
        stmt.setString(6, (String) customer.get("c_NationalityCode"));
        // 执行sql语句
        rs = stmt.executeQuery();
        while (rs.next()) {
            map.put("c_Customer_id", rs.getString("c_Customer_id"));
            map.put("c_familyId", rs.getString("c_familyId"));

        }
        return map;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return map;
}

/**
 * 判断customer 是否组存在
 *
 * @param id
 * @param con
 * @param stmt
 * @param rs
 * @return
 */
public static boolean checkIsHaveMember(String c_customerID, String groupid, Connection con, PreparedStatement stmt,
                                        ResultSet rs) {
    String sql = "SELECT count(*) from app_fd_member_table where c_Customer_id=? AND c_group_id=?";
    int count = 0;
    try {
        stmt = con.prepareStatement(sql);
        stmt.setString(1, c_customerID);
        stmt.setString(2, groupid);
        // 执行sql语句
        rs = stmt.executeQuery();
        while (rs.next()) {
            count = rs.getInt(1);
        }
        return count > 0;
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return count > 0;
}

/**
 * 落地member数据到数据库中
 */
public static void loadMemberData(Map customerMap, Connection con, PreparedStatement stmt) {

    String insertCustomerSql = "insert into app_fd_member_table" + "(id,c_member_id,c_start_time,c_group_id,"
            + "c_Customer_id,c_phoneid,c_emailid,c_expiry_time,c_familyId,c_name,"
            + "c_IDNumber,c_dateofbirth,c_familyNo,dateCreated,createdBy)" + "values"
            + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    try {
        stmt = con.prepareStatement(insertCustomerSql);
        stmt.setString(1, "member" + String.valueOf(System.currentTimeMillis())
                + String.valueOf(((int) ((Math.random() * 9 + 1) * 100000))));
        stmt.setString(2, (String) ((Map) customerMap).get("c_member_id") == null ? ""
                : (String) ((Map) customerMap).get("c_member_id"));
        stmt.setString(3, (String) ((Map) customerMap).get("c_StartDate") == null ? ""
                : (String) ((Map) customerMap).get("c_StartDate"));
        stmt.setString(4, (String) ((Map) customerMap).get("c_groupid") == null ? ""
                : (String) ((Map) customerMap).get("c_groupid"));
        stmt.setString(5, (String) ((Map) customerMap).get("c_Customer_id") == null ? ""
                : (String) ((Map) customerMap).get("c_Customer_id"));
        stmt.setString(6, (String) ((Map) customerMap).get("c_phoneid") == null ? ""
                : (String) ((Map) customerMap).get("c_phoneid"));
        stmt.setString(7, (String) ((Map) customerMap).get("c_EmailAddress") == null ? ""
                : (String) ((Map) customerMap).get("c_EmailAddress"));
        stmt.setString(8, (String) ((Map) customerMap).get("c_ExpiryDate") == null ? ""
                : (String) ((Map) customerMap).get("c_ExpiryDate"));
        stmt.setString(9, (String) ((Map) customerMap).get("c_family_id") == null ? ""
                : (String) ((Map) customerMap).get("c_family_id"));
        stmt.setString(10,
                (String) ((Map) customerMap).get("c_Forename") + " " + ((Map) customerMap).get("c_Surname"));
        stmt.setString(11, (String) ((Map) customerMap).get("c_IDNumber") == null ? ""
                : (String) ((Map) customerMap).get("c_IDNumber"));
        stmt.setString(12, (String) ((Map) customerMap).get("c_DateofBirth") == null ? ""
                : (String) ((Map) customerMap).get("c_DateofBirth"));
        stmt.setString(13, (String) ((Map) customerMap).get("familyNo") == null ? ""
                : (String) ((Map) customerMap).get("familyNo"));
        stmt.setString(14, (String) ((Map) customerMap).get("dateCreated") == null ? ""
                : (String) ((Map) customerMap).get("dateCreated"));
        stmt.setString(15, (String) ((Map) customerMap).get("createdBy") == null ? ""
                : (String) ((Map) customerMap).get("createdBy"));

        int row = stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }

}

// 计算入保年龄方法
public static String countPremiumAge(String c_CensusType, String c_StartDate, String c_DateofBirth) {
    Logger logger = Logger.getLogger("MyCustomLogger");
    logger.info("in countPremiumAge.(line:1137)");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat sdfGr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date startDate;
    long age = 0l;
    try {
        if ("new".equalsIgnoreCase(c_CensusType)) {
            startDate = sdfGr.parse(c_StartDate);
        } else {
            startDate = sdf.parse(c_StartDate);
        }

        long startDateLong = startDate.getTime();// 入保时间
        Date dateofBirth = sdf.parse(c_DateofBirth);
        long birthLong = dateofBirth.getTime();// 入保时间
        Date currentTiem = new Date();
        long currentLong = currentTiem.getTime();
        long yearLong = startDateLong - birthLong; // 31536000000
        long timedur = 1000 * 60 * 60 * 24 * 365l;
        age = yearLong / timedur;
    } catch (ParseException e) {
        logger.info("exception in countPremiumAge.(line:1158)");
        e.printStackTrace();
    }
    logger.info("end countPremiumAge.(line:1161)");
    return String.valueOf(age);
}

// 计算保费
public static String countPremiumMoney(List listAgesMoney, String ageband) {
    int age = Integer.parseInt(ageband);
    String money = "";
    for (Object str : listAgesMoney) {
        int arr0 = Integer.parseInt(((String) str).split(":")[0]);
        int arr1 = Integer.parseInt(((String) str).split(":")[1]);
        if (age >= arr0 && age <= arr1) {
            money = ((String) str).split(":")[2];
            break;
        }
    }
    return money;
}

// 清空临时表的数据
public static void deleteCustomer(String userName, Connection con, PreparedStatement stmt) {
    Logger logger = Logger.getLogger("MyCustomLogger");
    logger.info("deleteCustomer.");
    String deleteCustomerSql = "delete from app_fd_customerimport_table where createdBy=?";
    try {
        stmt = con.prepareStatement(deleteCustomerSql);
        stmt.setString(1, userName);
        int row = stmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }

}

// 将保费落地
public static void savePremiumData(Map premiumMap, Connection con) {
    String sqlinsertPremium = "insert into app_fd_premium_table" + "(id,c_start_time,"
            + "c_group_id,c_currency,c_expiry_time,"
            + "c_customer_id,c_email,c_premium,c_age,c_gender,c_NationalityCode,"
            + "c_Nationality,c_name,c_IDNumber,c_Location,c_premiumtype,c_family_id,c_refund,dateCreated,createdBy,c_member_id,c_costtype,c_refund_status)"
            + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    PreparedStatement stmt;
    try {
        stmt = con.prepareStatement(sqlinsertPremium);

        stmt.setString(1, "PREMIUM" + String.valueOf(System.currentTimeMillis())
                + String.valueOf(((int) ((Math.random() * 9 + 1) * 100000))));
        String startDate = (String) ((Map) premiumMap).get("c_StartDate");
        stmt.setString(2, startDate.endsWith("00:00:00") ? startDate : startDate + " 00:00:00");
        stmt.setString(3, (String) ((Map) premiumMap).get("groupId"));
        stmt.setString(4, (String) ((Map) premiumMap).get("c_currency"));
        stmt.setString(5, (String) ((Map) premiumMap).get("c_ExpiryDate"));
        stmt.setString(6, (String) ((Map) premiumMap).get("c_Customer_id"));
        stmt.setString(7, (String) ((Map) premiumMap).get("c_EmailAddress"));
        stmt.setString(8, String.valueOf(((Map) premiumMap).get("premium")));
        stmt.setString(9, (String) ((Map) premiumMap).get("age"));
        stmt.setString(10, (String) ((Map) premiumMap).get("c_GenderMaleFemale"));
        stmt.setString(11, (String) ((Map) premiumMap).get("c_NationalityCode"));
        stmt.setString(12, (String) ((Map) premiumMap).get("c_Nationality"));
        stmt.setString(13,
                (String) ((Map) premiumMap).get("c_Forename") + " " + ((Map) premiumMap).get("c_Surname"));
        stmt.setString(14, (String) ((Map) premiumMap).get("c_IDNumber"));
        stmt.setString(15, (String) ((Map) premiumMap).get("c_Location"));
        stmt.setString(16, (String) ((Map) premiumMap).get("c_premiumtype"));
        stmt.setString(17, (String) ((Map) premiumMap).get("c_family_id"));
        stmt.setString(18, "0.00");
        stmt.setString(19, (String) ((Map) premiumMap).get("dateCreated") == null ? ""
                : (String) ((Map) premiumMap).get("dateCreated"));
        stmt.setString(20, (String) ((Map) premiumMap).get("createdBy") == null ? ""
                : (String) ((Map) premiumMap).get("createdBy"));
        stmt.setString(21, (String) ((Map) premiumMap).get("c_member_id") == null ? ""
                : (String) ((Map) premiumMap).get("c_member_id"));
        stmt.setString(22,"Premium");
        stmt.setString(23,"Unpaid");
        int row = stmt.executeUpdate();

    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// 加保逻辑
public static void addInsurance(String c_CensusType, Map paramMap, Map premiumMap, Map customerMap, Connection con,
                                PreparedStatement stmt, ResultSet rs) {

    String premiumType = (String) paramMap.get("premiumType");// 保费公式类型
    String currmemberType = (String) paramMap.get("MemberType");// 家庭类型
    String c_premiumid = (String) paramMap.get("c_premiumid");// 保费公式
    String familyId = (String) paramMap.get("familyId");// 家庭Id
    String groupStartTime = (String) paramMap.get("c_startTime");// group开始时间
    String groupExpiryTime = (String) paramMap.get("c_expiry_time");// group到期时间
    String[] c_memberarray = ((String) paramMap.get("c_member_id")).split("-");
    String memberfore = "";
    for (int i = 0; i < c_memberarray.length - 1; i++) {
        if (i == 0) {
            memberfore += c_memberarray[i];
        } else {
            memberfore += "-" + c_memberarray[i];
        }
    }
    /*
     * String memberfore = ((String)
     * paramMap.get("c_member_id")).split("-")[0] + "-" + ((String)
     * paramMap.get("c_member_id")).split("-")[1] + "-" + ((String)
     * paramMap.get("c_member_id")).split("-")[2] + "-" + ((String)
     * paramMap.get("c_member_id")).split("-")[3];
     */
    String memberEnd = "";
    // 1.根据familyId 查出家庭成员
    List listCustomers = findCustomersByfamilyId(familyId, con, stmt, rs);
    Map countFamily = new HashMap();
    Map countBeforFamily = new HashMap();
    if ("Age-Band".equalsIgnoreCase(premiumType)) {
        // 1.获取保费公式
        String[] arrages = c_premiumid.split("@");
        List listAgesMoney = new ArrayList();
        for (int i = 0; i < arrages.length; i++) {
            listAgesMoney.add(arrages[i]);
        }
        // 根据group 的入保起始时间得到入保年龄
        String age = countPremiumAge(c_CensusType, (String) customerMap.get("c_StartDate"),
                (String) customerMap.get("c_DateofBirth"));
        premiumMap.put("age", age);
        String premiumMoney = countPremiumMoney(listAgesMoney, age);
        // 根据时间入保时间按天单位保费计算实际保费
        premiumMoney = duePremiumMoney(groupStartTime, groupExpiryTime, premiumMoney,
                (String) customerMap.get("c_StartDate"));
        premiumMap.put("premium", premiumMoney);
        premiumMap.put("c_premiumtype", premiumType);
        premiumMap.put("c_family_id", familyId);
        premiumMap.put("dateCreated", getCurrentTime());
        premiumMap.put("createdBy", paramMap.get("userName"));
        premiumMap.put("c_Customer_id", customerMap.get("c_Customer_id"));
        memberEnd = countFamily(countFamily, listCustomers, memberEnd, currmemberType);
        // 生成卡片信息,
        String memberId = memberfore + "-" + memberEnd;
        premiumMap.put("c_member_id", memberId);
        savePremiumData(premiumMap, con);

        customerMap.put("c_member_id", memberId);
        loadMemberData(customerMap, con, stmt);

    } else {// Family-Rate 加保逻辑
        countLogFamily(countBeforFamily, listCustomers);
        // 2.判断家庭成员是属于公式的哪个类型，并算出保费。
        memberEnd = countFamily(countFamily, listCustomers, memberEnd, currmemberType);
        String memberbeforType = checkPremiumInFamily(countBeforFamily);
        String memberType = checkPremiumInFamily(countFamily);
        String memberId = memberfore + "-" + memberEnd;
        String premiumMoney = null;
        if (!memberbeforType.equalsIgnoreCase(memberType)) {
            premiumMoney = countPremiumInFamily(c_premiumid, countFamily);
            // 获取差额，首先获取这个家庭的保费之和
            List listPremium = findPermiumsByfamilyId(familyId, con, stmt, rs);
            double sumPre = 0.0d;
            for (Object obj : listPremium) {
                sumPre += Double.parseDouble((String) obj);
            }
            double duePremiumMoney = Double.parseDouble(premiumMoney) - sumPre;
            premiumMoney = duePremiumMoney(groupStartTime, groupExpiryTime, String.valueOf(duePremiumMoney),
                    (String) customerMap.get("c_StartDate"));
        } else {
            premiumMoney = "0.00";
        }
        premiumMap.put("premium", premiumMoney);
        premiumMap.put("c_premiumtype", premiumType);
        premiumMap.put("c_family_id", familyId);
        premiumMap.put("dateCreated", getCurrentTime());
        premiumMap.put("createdBy", paramMap.get("userName"));
        // 根据group 的入保起始时间得到入保年龄
        String age = countPremiumAge(c_CensusType, (String) customerMap.get("c_StartDate"),
                (String) customerMap.get("c_DateofBirth"));
        premiumMap.put("age", age);
        // 3.新增保费数据
        premiumMap.put("c_member_id", ((String) paramMap.get("c_member_id")));
        savePremiumData(premiumMap, con);
        customerMap.put("c_member_id", memberId);

        loadMemberData(customerMap, con, stmt);

    }

}

// 通过excel 手写的家庭编码查询对应的家庭Id
public static Map findFamilyIdByFamilyNo(String familyNo, String groupId, Connection con, PreparedStatement stmt,
                                         ResultSet rs) {
    String sqlSumMember = " SELECT * from app_fd_member_table t WHERE"
            + " c_group_id=? and t.c_familyNo =? and t.c_member_id like ? ";
    String c_family_id = "";
    Map map = new HashMap();
    try {
        stmt = con.prepareStatement(sqlSumMember);
        stmt.setString(1, groupId);
        stmt.setString(2, familyNo);
        stmt.setString(3, "%" + familyNo + "_EE");
        rs = stmt.executeQuery();

        while (rs.next()) {
            map.put("c_family_id", rs.getString("c_familyId"));
            map.put("c_member_id", rs.getString("c_member_id"));

        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return map;
}

// 根据familyId 查询家庭成员
public static List findCustomersByfamilyId(String c_familyId, Connection con, PreparedStatement stmt,
                                           ResultSet rs) {
    List customesList = new ArrayList();
    String sqlfindCus = "SELECT  * from app_fd_duecustomer_table WHERE" + " c_familyId=?";
    try {
        stmt = con.prepareStatement(sqlfindCus);
        stmt.setString(1, c_familyId);

        rs = stmt.executeQuery();
        // 保费数据记录
        while (rs.next()) {
            Map customerObject = new HashMap();
            customerObject.put("c_MemberType", rs.getString("c_MemberType"));
            customesList.add(customerObject);
        }
        System.out.println("eeee");
    } catch (Exception e) {
    }
    return customesList;
}

public static String countFamily(Map countFamily, List listCustomers, String memberEnd, String currmemberType) {
    int employee = 0;
    int spouse = 0;
    int children = 0;

    for (Object customer : listCustomers) {
        String memberType = (String) ((Map) customer).get("c_MemberType");
        System.out.print("memberType:   ===:" + memberType);
        if ("Employee".equalsIgnoreCase(memberType)) {
            employee++;
            countFamily.put("Employee", employee);
        } else if ("Spouse".equalsIgnoreCase(memberType)) {
            spouse++;
            countFamily.put("Spouse", spouse);
        } else if ("Children".equalsIgnoreCase(memberType)) {
            children++;
            countFamily.put("Children", children);

        }
    }
    if ("Spouse".equalsIgnoreCase(currmemberType)) {
        ++spouse;
        countFamily.put("Spouse", spouse);
        memberEnd = "SP";
    } else if ("Children".equalsIgnoreCase(currmemberType)) {
        ++children;
        countFamily.put("Children", children);
        memberEnd = "C" + children;
    }
    return memberEnd;
}

// 之前的家庭类型
public static void countLogFamily(Map countBeforFamily, List listCustomers) {
    int employee = 0;
    int spouse = 0;
    int children = 0;

    for (Object customer : listCustomers) {
        String memberType = (String) ((Map) customer).get("c_MemberType");
        System.out.print("memberType:   ===:" + memberType);
        if ("Employee".equalsIgnoreCase(memberType)) {
            employee++;
            countBeforFamily.put("Employee", employee);
        } else if ("Spouse".equalsIgnoreCase(memberType)) {
            spouse++;
            countBeforFamily.put("Spouse", spouse);
        } else if ("Children".equalsIgnoreCase(memberType)) {
            children++;
            countBeforFamily.put("Children", children);

        }
    }
    return;

}

// 根据家庭类型计算保费
public static String countPremiumInFamily(String premium, Map familyCount) {

    String[] familyrate = premium.split("@");
    String employeeonly = familyrate[0];// 光棍
    String couple = familyrate[1];// 单亲加小孩（小孩个数不限）
    String family = familyrate[2];// 夫妻加小孩（小孩个数不限）
    String employeeonlymoney = employeeonly.split(":")[1];
    String couplespf_rate = couple.split(":")[1];
    String family_rate = family.split(":")[1];

    // 对familyCount 计算保费
    // 先根据年龄算出 employyee的保费然后根据系数算出家人的保费，得到总保费。
    int Employee = (int) (familyCount.get("Employee") == null ? 0 : (int) familyCount.get("Employee"));
    int Spouse = (int) (familyCount.get("Spouse") == null ? 0 : (int) familyCount.get("Spouse"));
    int Children = (int) (familyCount.get("Children") == null ? 0 : (int) familyCount.get("Children"));
    double premiumMoney = 0.00d;
    if (Employee == 1 && Spouse == 0 && Children == 0) {
        premiumMoney = Double.valueOf(employeeonlymoney);
    } else if (Employee == 1 && Spouse == 1 && Children == 0) {
        premiumMoney = Double.valueOf(employeeonlymoney) * Double.valueOf(couplespf_rate);
    } else if (Employee == 1 && Spouse == 0 && Children > 0) {
        premiumMoney = Double.valueOf(employeeonlymoney) * Double.valueOf(couplespf_rate);
    } else if (Employee == 1 && Spouse > 0 && Children > 0) {
        premiumMoney = Double.valueOf(employeeonlymoney) * Double.valueOf(family_rate);
    }

    String bigpremiumMoney = new BigDecimal(premiumMoney).setScale(2, BigDecimal.ROUND_UP).toString();
    return bigpremiumMoney;

}

// 判断加保前的家庭属于什么类型
public static String checkPremiumInFamily(Map familyCount) {
    int Employee = (int) (familyCount.get("Employee") == null ? 0 : (int) familyCount.get("Employee"));
    int Spouse = (int) (familyCount.get("Spouse") == null ? 0 : (int) familyCount.get("Spouse"));
    int Children = (int) (familyCount.get("Children") == null ? 0 : (int) familyCount.get("Children"));
    String memberType = null;
    if (Employee == 1 && Spouse == 0 && Children == 0) {
        memberType = "EE";
    } else if (Employee == 1 && Spouse == 1 && Children == 0) {
        memberType = "SP";
    } else if (Employee == 1 && Spouse == 0 && Children > 0) {
        memberType = "SP";
    } else if (Employee == 1 && Spouse > 0 && Children > 0) {
        memberType = "CH";
    }

    return memberType;

}

// 计算加保费用
public static String duePremiumMoney(String groupStartTime, String groupExpiryTime, String premiumMoney,
                                     String customerStartTime) {
    SimpleDateFormat sdfGroup = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Calendar calendar = Calendar.getInstance();
    double returnd = 0.0d;
    String returnMoney = "";
    try {
        calendar.setTime(sdfGroup.parse(groupExpiryTime));
        long groupEtM = calendar.getTimeInMillis();
        calendar.setTime(sdfGroup.parse(groupStartTime));
        long groupStart = calendar.getTimeInMillis();
        calendar.setTime(sdf.parse(customerStartTime));
        long doExpiry = calendar.getTimeInMillis();
        long groupBetweenDays = (groupEtM - groupStart) / (1000 * 3600 * 24) + 1;
        long returnBetweenDays = (groupEtM - doExpiry) / (1000 * 3600 * 24) + 1;
        double dayMoney = Double.valueOf(premiumMoney.replaceAll(",", "")) / groupBetweenDays;
        returnd = returnBetweenDays * dayMoney;
        returnMoney = new BigDecimal(String.valueOf(returnd)).setScale(2, BigDecimal.ROUND_UP).toString();
    } catch (Exception e) {
        e.printStackTrace();
    }
    return returnMoney;

}

// 根据familyId 保费记录
public static List findPermiumsByfamilyId(String c_familyId, Connection con, PreparedStatement stmt, ResultSet rs) {
    List premiumList = new ArrayList();
    String sqlfindCus = "SELECT  * from app_fd_premium_table WHERE" + " c_family_id=?";
    try {
        stmt = con.prepareStatement(sqlfindCus);
        stmt.setString(1, c_familyId);

        rs = stmt.executeQuery();
        // 保费数据记录
        while (rs.next()) {
            premiumList.add(rs.getString("c_premium"));
        }
    } catch (Exception e) {
    }
    return premiumList;
}

/**
 * 查询保费Employee
 *
 * @return
 */
public static void setPremiumByFamilyId(Map premiumMap, String groupId, Map map, Connection con,
                                        PreparedStatement stmt, ResultSet rs) {
    try {

        String sqlfndpre = "SELECT  * from app_fd_premium_table WHERE c_group_id=? AND  c_family_id=?";
        stmt = con.prepareStatement(sqlfndpre);
        stmt.setString(1, groupId);
        stmt.setString(2, (String) map.get("c_family_id"));
        rs = stmt.executeQuery();
        // 保费数据记录
        while (rs.next()) {
            premiumMap.put("c_ExpiryDate", rs.getString("c_expiry_time"));
            premiumMap.put("c_EmailAddress", rs.getString("c_email"));
            premiumMap.put("c_GenderMaleFemale", rs.getString("c_gender"));
            premiumMap.put("c_NationalityCode", rs.getString("c_NationalityCode"));
            premiumMap.put("c_Nationality", rs.getString("c_Nationality"));
            if (rs.getString("c_name") != null && rs.getString("c_name") != "") {
                premiumMap.put("c_Forename", (rs.getString("c_name").split(" "))[0]);
                premiumMap.put("c_Surname", (rs.getString("c_name").split(" "))[1]);
            } else {
                premiumMap.put("c_Forename", "");
                premiumMap.put("c_Surname", "");
            }
            premiumMap.put("c_IDNumber", rs.getString("c_IDNumber"));
            premiumMap.put("c_Location", rs.getString("c_Location"));
            premiumMap.put("c_premiumtype", rs.getString("c_premiumtype"));
            premiumMap.put("c_Customer_id", rs.getString("c_customer_id"));
            break;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return;
}

public static String getCurrentTime() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String nowDate = sdf.format(new Date());
    return nowDate;

}

// 获取年份后两位 CN-HCCN-2020-01-0001
public static String getYearHalf(String groupId) {
    String[] grArr=groupId.split("-");
    return grArr[grArr.length-3].substring(2);
}






public  Connection getConnection() {
    Connection con=null;
    try {
        try {
            DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return con;
}

// 关闭连接
public static void CloseCon(Connection con, PreparedStatement stmt, ResultSet rs) {
    if (con != null) {
        try {
            con.close();
            if (stmt != null) {
                stmt.close();
            }
            if (rs != null) {
                rs.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

return createPremium();