export const ME = {
  id: 'me',
  name: 'Riley Chen',
  email: 'riley@gmail.com',
  handle: '@riley',
  bg: '#12B35E',
}

export const PEOPLE = {
  me:     ME,
  priya:  { id: 'priya',  name: 'Priya Shah',     email: 'priya.s@gmail.com',   bg: '#FF7A66' },
  jordan: { id: 'jordan', name: 'Jordan Lee',      email: 'jordan@proton.me',    bg: '#9C7BFF' },
  sam:    { id: 'sam',    name: 'Sam Okafor',      email: 'sam.o@icloud.com',    bg: '#5EC8F2' },
  maya:   { id: 'maya',   name: 'Maya Rodriguez',  email: 'maya.rod@gmail.com',  bg: '#FFD85C' },
  theo:   { id: 'theo',   name: 'Theo Nakamura',   email: 'theon@hey.com',       bg: '#34D6B8' },
  alex:   { id: 'alex',   name: 'Alex Fischer',    email: 'alex.f@outlook.com',  bg: '#FF8AC7' },
  noor:   { id: 'noor',   name: 'Noor Hassan',     email: 'noor@gmail.com',      bg: '#FFB26B' },
}

const P = PEOPLE

export const GROUPS = [
  {
    id: 'lisbon',
    name: 'Lisbon weekend',
    emoji: '✈️',
    color: 'linear-gradient(160deg, #35C974 0%, #12B35E 60%, #077540 100%)',
    solidColor: '#12B35E',
    members: [P.me, P.priya, P.jordan, P.sam, P.maya, P.theo],
    youOwe: 42.50,
    youAreOwed: 0,
    total: 1842.30,
    lastActivity: '2h ago',
  },
  {
    id: 'apt4b',
    name: 'Apartment 4B',
    emoji: '🏠',
    color: '#34D6B8',
    solidColor: '#34D6B8',
    members: [P.me, P.jordan, P.alex],
    youOwe: 0,
    youAreOwed: 128.40,
    total: 3240.18,
    lastActivity: 'Yesterday',
  },
  {
    id: 'bookclub',
    name: 'Book club dinners',
    emoji: '📚',
    color: '#9C7BFF',
    solidColor: '#9C7BFF',
    members: [P.me, P.priya, P.noor, P.maya],
    youOwe: 0,
    youAreOwed: 18.25,
    total: 312.00,
    lastActivity: '4 days ago',
  },
  {
    id: 'ski',
    name: 'Tahoe ski trip',
    emoji: '🎿',
    color: '#5EC8F2',
    solidColor: '#5EC8F2',
    members: [P.me, P.priya, P.jordan, P.sam, P.theo, P.alex, P.noor],
    youOwe: 0,
    youAreOwed: 0,
    total: 4180.22,
    lastActivity: 'Settled Mar 18',
    settled: true,
  },
]

export const EXPENSES = {
  lisbon: [
    { id: 'e1', title: 'Time Out Market dinner',  category: 'food',          amount: 186.00, paidBy: 'priya',  date: 'Today · 8:42 PM',  split: 'equal', note: 'Shared 4 plates + wine' },
    { id: 'e2', title: 'Uber to Alfama',          category: 'travel',        amount:  24.50, paidBy: 'me',     date: 'Today · 6:15 PM',  split: 'equal' },
    { id: 'e3', title: 'Pastéis de Belém',        category: 'food',          amount:  18.40, paidBy: 'jordan', date: 'Today · 2:30 PM',  split: 'equal' },
    { id: 'e4', title: 'Airbnb — 2 nights',       category: 'rent',          amount: 624.00, paidBy: 'sam',    date: 'Yesterday',        split: 'equal' },
    { id: 'e5', title: 'Fado show tickets',       category: 'entertainment', amount: 180.00, paidBy: 'maya',   date: 'Yesterday',        split: 'equal' },
    { id: 'e6', title: 'Groceries — Continente',  category: 'groceries',     amount:  67.40, paidBy: 'theo',   date: 'Fri, Apr 11',      split: 'equal' },
    { id: 'e7', title: 'Taxi from airport',       category: 'travel',        amount:  42.00, paidBy: 'priya',  date: 'Fri, Apr 11',      split: 'equal' },
    { id: 'e8', title: 'Coffee + pastries',       category: 'food',          amount:  22.80, paidBy: 'jordan', date: 'Fri, Apr 11',      split: 'equal' },
  ],
  apt4b: [
    { id: 'a1', title: 'Electric bill — March',   category: 'utilities', amount: 142.18, paidBy: 'me',     date: 'Apr 1',   split: 'equal' },
    { id: 'a2', title: 'Internet',                category: 'utilities', amount:  65.00, paidBy: 'me',     date: 'Apr 1',   split: 'equal' },
    { id: 'a3', title: 'Costco run',              category: 'groceries', amount: 184.32, paidBy: 'alex',   date: 'Mar 29',  split: 'equal' },
    { id: 'a4', title: 'Rent — April',            category: 'rent',      amount: 2800.00,paidBy: 'jordan', date: 'Mar 28',  split: 'equal' },
    { id: 'a5', title: 'Toilet paper + cleaning', category: 'other',     amount:  48.68, paidBy: 'me',     date: 'Mar 24',  split: 'equal' },
  ],
  bookclub: [
    { id: 'b1', title: "Maya's house potluck",    category: 'food',  amount:  73.00, paidBy: 'maya',  date: 'Apr 10', split: 'equal' },
    { id: 'b2', title: 'Wine for the month',      category: 'food',  amount: 112.00, paidBy: 'priya', date: 'Apr 3',  split: 'equal' },
    { id: 'b3', title: 'New books — Strand',      category: 'other', amount: 127.00, paidBy: 'noor',  date: 'Mar 28', split: 'equal' },
  ],
  ski: [],
}

export const ACTIVITY = [
  { id: 'n1', type: 'expense', who: 'priya',  title: 'added "Time Out Market dinner"', amount: 186.00, group: 'Lisbon weekend', yourShare: -31.00,  time: '2h ago' },
  { id: 'n2', type: 'expense', who: 'me',     title: 'added "Uber to Alfama"',         amount:  24.50, group: 'Lisbon weekend', yourShare:  20.42,  time: '3h ago' },
  { id: 'n3', type: 'payment', who: 'alex',   title: 'paid you',                       amount:  64.00, group: 'Apartment 4B',  yourShare:  64.00,  time: 'Yesterday' },
  { id: 'n4', type: 'expense', who: 'jordan', title: 'added "Pastéis de Belém"',       amount:  18.40, group: 'Lisbon weekend', yourShare:  -3.07,  time: 'Yesterday' },
  { id: 'n5', type: 'expense', who: 'sam',    title: 'added "Airbnb — 2 nights"',      amount: 624.00, group: 'Lisbon weekend', yourShare: -104.00, time: 'Yesterday' },
  { id: 'n6', type: 'payment', who: 'me',     title: 'paid Jordan',                    amount: 125.00, group: 'Apartment 4B',  yourShare: -125.00, time: '2 days ago' },
  { id: 'n7', type: 'expense', who: 'maya',   title: 'added "Fado show tickets"',      amount: 180.00, group: 'Lisbon weekend', yourShare:  -30.00, time: '2 days ago' },
  { id: 'n8', type: 'group',   who: 'me',     title: 'created group "Book club dinners"',             group: 'Book club dinners',         time: 'Mar 1' },
]

export const FRIENDS = [
  { id: 'priya',  balance:  -42.50, shared: ['Lisbon weekend', 'Book club dinners'] },
  { id: 'jordan', balance:  +68.20, shared: ['Lisbon weekend', 'Apartment 4B'] },
  { id: 'sam',    balance: -104.00, shared: ['Lisbon weekend', 'Tahoe ski trip'] },
  { id: 'maya',   balance:  -30.00, shared: ['Lisbon weekend', 'Book club dinners'] },
  { id: 'theo',   balance:  -11.23, shared: ['Lisbon weekend', 'Tahoe ski trip'] },
  { id: 'alex',   balance:  +60.20, shared: ['Apartment 4B'] },
  { id: 'noor',   balance:  +18.25, shared: ['Book club dinners'] },
]

export const CATEGORIES = [
  { key: 'food',          emoji: '🍕', label: 'Food',          bg: '#FFE2DC', dot: '#FF7A66' },
  { key: 'travel',        emoji: '✈️', label: 'Travel',        bg: '#FFE7D1', dot: '#FFB26B' },
  { key: 'groceries',     emoji: '🛒', label: 'Groceries',     bg: '#FFF3C5', dot: '#FFD85C' },
  { key: 'utilities',     emoji: '💡', label: 'Utilities',     bg: '#D5EFFB', dot: '#5EC8F2' },
  { key: 'entertainment', emoji: '🎬', label: 'Entertainment', bg: '#E4DAFF', dot: '#9C7BFF' },
  { key: 'gifts',         emoji: '🎁', label: 'Gifts',         bg: '#FFDEF0', dot: '#FF8AC7' },
  { key: 'rent',          emoji: '🏠', label: 'Rent',          bg: '#C8F5EB', dot: '#34D6B8' },
  { key: 'other',         emoji: '📦', label: 'Other',         bg: '#E0E5EA', dot: '#8895A8' },
]

export const fmt = (n, signed = false) => {
  const abs = Math.abs(n).toFixed(2)
  const [w, f] = abs.split('.')
  const sign = signed ? (n > 0 ? '+ ' : n < 0 ? '− ' : '') : ''
  return `${sign}₹${Number(w).toLocaleString()}.${f}`
}
