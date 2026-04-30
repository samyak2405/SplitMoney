# Design Splitwise

## Requirements
### Functional Requirements
- Create user
- Create group
- Add members to group
- Add expense:
  - Equal split
  - Exact amount split
  - Percentage split
- Get balances
- Get simplified settlements (minimum transactions)
- View expense history

### Non-Functional 
- 100M users
- Groups up to 1000 members
- Heavy Read Traffic
- Strong consistency for balances
- Low Latency (< 200ms p95)

