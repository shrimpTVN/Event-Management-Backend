# Point Category Grouped & Flattened Tree Implementation Guide

## 1. Executive Summary & Problem Context

In training point evaluation interfaces (Đánh giá Điểm rèn luyện - ĐRL), the frontend UI presents:
1. **Main Categories Navigation** (Level 0 roots: Category I, II, III, IV, V) as top navigation tabs, stepper cards, or sidebar menus.
2. **Evaluation Sheet** as a flat scoring table/form rendered when a main category is clicked.

### The Pain Point with Pure Hierarchical Trees
In a standard recursive tree (`{ id, children: [ { id, children: [...] } ] }`), rendering the selected tab as a table requires the frontend to write custom recursive tree traversal logic just to flatten the rows for `<tr>` or list elements.

### The Solution: Option 2 (Grouped Roots + Pre-flattened DFS Descendants)
The backend provides **pre-grouped, pre-flattened sub-trees**:
- Top-level elements represent the **Main Categories** (Level 0 roots).
- Each root contains an `items` array of all its descendants, pre-sorted in **Depth-First Search (DFS) pre-order**.
- Each item includes `level` (for visual indent) and `isLeaf` (indicating if it is an evaluation leaf with an input field).

---

## 2. Target JSON Structure

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "I. Trách nhiệm, tinh thần và thái độ trong học tập",
      "note": "Mã tiêu chí: I",
      "maximum": 20,
      "dateApply": "2026-11-07",
      "level": 0,
      "parentCategoryId": null,
      "isActive": true,
      "childrenOrder": 1,
      "items": [
        {
          "id": 2,
          "name": "a. Trách nhiệm và thái độ tham gia các hoạt động học tập, NCKH",
          "note": "Mã tiêu chí: I.a",
          "maximum": 20,
          "dateApply": "2026-11-07",
          "level": 1,
          "parentCategoryId": 1,
          "isActive": true,
          "childrenOrder": 1,
          "isLeaf": false
        },
        {
          "id": 3,
          "name": "Đi học đầy đủ, đúng giờ, nghiêm túc trong giờ học",
          "note": "Mỗi học phần bị cấm thi trừ 03 điểm",
          "maximum": 6,
          "dateApply": "2026-11-07",
          "level": 2,
          "parentCategoryId": 2,
          "isActive": true,
          "childrenOrder": 1,
          "isLeaf": true
        },
        {
          "id": 4,
          "name": "Nghiên cứu khoa học (NCKH)",
          "note": "Mã tiêu chí: I.a.2",
          "maximum": 8,
          "dateApply": "2026-11-07",
          "level": 2,
          "parentCategoryId": 2,
          "isActive": true,
          "childrenOrder": 2,
          "isLeaf": false
        },
        {
          "id": 5,
          "name": "Có tham gia đề tài NCKH của đơn vị đào tạo",
          "note": "Không tính bài tập, tiểu luận...",
          "maximum": 5,
          "dateApply": "2026-11-07",
          "level": 3,
          "parentCategoryId": 4,
          "isActive": true,
          "childrenOrder": 1,
          "isLeaf": true
        }
      ]
    }
  ]
}
```

---

## 3. Frontend Usage Example

Frontend code becomes straightforward with zero recursion:

```tsx
// 1. Render Tabs for Main Categories
<Tabs value={selectedTabId} onChange={setSelectedTabId}>
  {data.map(root => (
    <Tab key={root.id} value={root.id} label={`${root.name} (Max ${root.maximum}đ)`} />
  ))}
</Tabs>

// 2. Render Flat Scoring Sheet for Active Tab
{selectedCategory.items.map(item => (
  <tr key={item.id} className={item.isLeaf ? "leaf-row" : "group-header-row"}>
    <td style={{ paddingLeft: `${item.level * 20}px` }}>
      {item.name}
      {item.note && <span className="text-muted block text-sm">{item.note}</span>}
    </td>
    <td>{item.maximum}</td>
    <td>
      {item.isLeaf ? (
        <input type="number" max={item.maximum} min={0} />
      ) : (
        <span className="font-semibold text-gray-500">—</span>
      )}
    </td>
  </tr>
))}
```

---

## 4. Architecture & Layer Boundaries

```
[ddd-api]
  ├── PointCategoryController.java (GET /point-categories/grouped-tree)
  ├── PointCategoryGroupedTreeResponseDto.java
  ├── PointCategoryFlattenedItemResponseDto.java
  └── PointCategoryApiMapper.java
        │
        ▼
[ddd-application]
  ├── PointCategoryService.java (getGroupedPointCategoryTree)
  ├── PointCategoryServiceImpl.java (DFS pre-order tree traversal)
  ├── PointCategoryGroupedTreeDto.java
  └── PointCategoryFlattenedItemDto.java
        │
        ▼
[ddd-domain]
  ├── PointCategory.java
  └── PointCategoryRepository.java
```

---

## 5. Step-by-Step Implementation Details

### 5.1. Application Layer DTOs

#### File: `ddd-application/src/main/java/com/ddd/application/dto/pointcategory/PointCategoryFlattenedItemDto.java`
```java
package com.ddd.application.dto.pointcategory;

import java.time.LocalDate;

public record PointCategoryFlattenedItemDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive,
        Integer childrenOrder,
        boolean isLeaf
) {}
```

#### File: `ddd-application/src/main/java/com/ddd/application/dto/pointcategory/PointCategoryGroupedTreeDto.java`
```java
package com.ddd.application.dto.pointcategory;

import java.time.LocalDate;
import java.util.List;

public record PointCategoryGroupedTreeDto(
        Long id,
        String name,
        String note,
        Integer maximum,
        LocalDate dateApply,
        Integer level,
        Long parentCategoryId,
        boolean isActive,
        Integer childrenOrder,
        List<PointCategoryFlattenedItemDto> items
) {}
```

---

### 5.2. Service Implementation

#### Service Interface: `PointCategoryService.java`
```java
List<PointCategoryGroupedTreeDto> getGroupedPointCategoryTree();
```

#### Service Implementation: `PointCategoryServiceImpl.java`
```java
@Transactional(readOnly = true)
@Override
public List<PointCategoryGroupedTreeDto> getGroupedPointCategoryTree() {
    List<PointCategory> allCategories = pointCategoryRepository.findAll();
    List<PointCategory> activeCategories = allCategories.stream()
            .filter(PointCategory::isActive)
            .toList();

    Map<Long, List<PointCategory>> childrenByParent = activeCategories.stream()
            .filter(c -> c.getParentCategoryId() != null && c.getParentCategoryId() != 0L)
            .collect(Collectors.groupingBy(PointCategory::getParentCategoryId));

    List<PointCategory> roots = activeCategories.stream()
            .filter(c -> c.getParentCategoryId() == null || c.getParentCategoryId() == 0L)
            .sorted(POINT_CATEGORY_COMPARATOR)
            .toList();

    return roots.stream()
            .map(root -> {
                List<PointCategoryFlattenedItemDto> flatItems = new ArrayList<>();
                flattenDescendantsDfs(root.getId(), childrenByParent, flatItems);
                return new PointCategoryGroupedTreeDto(
                        root.getId(),
                        root.getName(),
                        root.getNote(),
                        root.getMaximum(),
                        root.getDateApply(),
                        root.getLevel(),
                        root.getParentCategoryId(),
                        root.isActive(),
                        root.getChildrenOrder(),
                        flatItems
                );
            })
            .toList();
}

private void flattenDescendantsDfs(Long parentId,
                                  Map<Long, List<PointCategory>> childrenByParent,
                                  List<PointCategoryFlattenedItemDto> resultList) {
    List<PointCategory> directChildren = childrenByParent.getOrDefault(parentId, List.of())
            .stream()
            .sorted(POINT_CATEGORY_COMPARATOR)
            .toList();

    for (PointCategory child : directChildren) {
        boolean isLeaf = !childrenByParent.containsKey(child.getId()) 
                || childrenByParent.get(child.getId()).isEmpty();

        resultList.add(new PointCategoryFlattenedItemDto(
                child.getId(),
                child.getName(),
                child.getNote(),
                child.getMaximum(),
                child.getDateApply(),
                child.getLevel(),
                child.getParentCategoryId(),
                child.isActive(),
                child.getChildrenOrder(),
                isLeaf
        ));

        flattenDescendantsDfs(child.getId(), childrenByParent, resultList);
    }
}
```

---

### 5.3. API Layer Response DTOs & Mapper

#### Response DTOs:
- `PointCategoryFlattenedItemResponseDto.java`: Mirror of `PointCategoryFlattenedItemDto`.
- `PointCategoryGroupedTreeResponseDto.java`: Mirror of `PointCategoryGroupedTreeDto` containing `List<PointCategoryFlattenedItemResponseDto> items`.

#### MapStruct Mapper: `PointCategoryApiMapper.java`
```java
PointCategoryGroupedTreeResponseDto toPointCategoryGroupedTreeResponseDto(PointCategoryGroupedTreeDto dto);
List<PointCategoryGroupedTreeResponseDto> toPointCategoryGroupedTreeResponseDtoList(List<PointCategoryGroupedTreeDto> dtos);
```

---

### 5.4. Controller Endpoint

In `PointCategoryController.java`:
```java
@GetMapping("/grouped-tree")
public BaseResponse<List<PointCategoryGroupedTreeResponseDto>> getGroupedPointCategoryTree() {
    List<PointCategoryGroupedTreeDto> tree = pointCategoryService.getGroupedPointCategoryTree();
    return BaseResponse.of(pointCategoryApiMapper.toPointCategoryGroupedTreeResponseDtoList(tree));
}
```
*(Note: We can also provide this as `@GetMapping("/tree")` or keep both endpoints depending on frontend preference).*

---

## 6. Testing & Quality Assurance Plan

1. **Hierarchy Preservation**: Verify direct children appear immediately following their parent, preceding siblings.
2. **isLeaf Accuracy**: Verify leaf nodes correctly set `isLeaf = true` and parent nodes set `isLeaf = false`.
3. **Soft-deleted Filter**: Verify inactive records (`isActive = false`) and their subtrees are excluded.
4. **Ordering**: Verify roots and children follow `level ASC`, `childrenOrder ASC`, `id ASC`.
